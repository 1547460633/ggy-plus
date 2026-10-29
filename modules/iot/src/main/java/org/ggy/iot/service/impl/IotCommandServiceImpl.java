package org.ggy.iot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.ggy.common.core.exception.ServiceException;
import org.ggy.common.core.utils.MapstructUtils;
import org.ggy.common.json.utils.JsonUtils;
import org.ggy.common.mybatis.core.page.PageQuery;
import org.ggy.common.mybatis.core.page.TableDataInfo;
import org.ggy.iot.config.IotMqttProperties;
import org.ggy.iot.domain.IotDevice;
import org.ggy.iot.domain.IotDeviceCommand;
import org.ggy.iot.domain.IotDeviceReport;
import org.ggy.iot.domain.bo.IotLightCommandBo;
import org.ggy.iot.domain.vo.IotDeviceCommandVo;
import org.ggy.iot.mapper.IotDeviceCommandMapper;
import org.ggy.iot.mapper.IotDeviceMapper;
import org.ggy.iot.mapper.IotDeviceReportMapper;
import org.ggy.iot.mqtt.IotMqttGateway;
import org.ggy.iot.service.IIotCommandService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/** IoT 指令状态机实现。@ggy */
@Service
@RequiredArgsConstructor
public class IotCommandServiceImpl implements IIotCommandService {
    private final IotDeviceMapper deviceMapper;
    private final IotDeviceCommandMapper commandMapper;
    private final IotDeviceReportMapper reportMapper;
    private final IotMqttGateway mqttGateway;
    private final IotMqttProperties properties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IotDeviceCommandVo switchLight(Long deviceId, IotLightCommandBo bo) {
        IotDevice device = deviceMapper.selectByIdForUpdate(deviceId);
        if (device == null) throw new ServiceException("设备不存在");
        IotDeviceCommand command = new IotDeviceCommand();
        command.setDeviceId(deviceId); command.setRequestId(UUID.randomUUID().toString());
        command.setCommandType("LIGHT_SWITCH"); command.setDesiredLightState(bo.getLightState());
        command.setTimeoutTime(new Date(System.currentTimeMillis() + properties.getCommandTimeoutSeconds() * 1000));
        if (!"0".equals(device.getEnabled())) {
            return saveFailure(command, "设备已停用");
        }
        if (!"1".equals(device.getOnlineStatus())) {
            return saveFailure(command, "设备离线");
        }
        boolean active = commandMapper.selectCount(new LambdaQueryWrapper<IotDeviceCommand>()
            .eq(IotDeviceCommand::getDeviceId, deviceId)
            .in(IotDeviceCommand::getCommandStatus, "PENDING", "PUBLISHED", "ACKED")) > 0;
        if (active) return saveFailure(command, "设备存在正在执行的指令");
        command.setCommandStatus("PENDING"); commandMapper.insert(command);
        try {
            String topic = properties.topic("devices/" + device.getDeviceKey() + "/commands/down");
            String payload = JsonUtils.toJsonString(Map.of("requestId", command.getRequestId(), "type", command.getCommandType(),
                "lightState", command.getDesiredLightState(), "issuedAt", System.currentTimeMillis()));
            mqttGateway.publishCommand(topic, payload.getBytes(StandardCharsets.UTF_8));
            command.setCommandStatus("PUBLISHED"); command.setPublishedTime(new Date()); commandMapper.updateById(command);
        } catch (Exception e) {
            command.setCommandStatus("FAILED"); command.setFailureReason("MQTT 发送失败：" + trim(e.getMessage())); command.setFinishedTime(new Date());
            commandMapper.updateById(command);
        }
        return MapstructUtils.convert(command, IotDeviceCommandVo.class);
    }

    private IotDeviceCommandVo saveFailure(IotDeviceCommand command, String reason) {
        command.setCommandStatus("FAILED"); command.setFailureReason(reason); command.setFinishedTime(new Date()); commandMapper.insert(command);
        return MapstructUtils.convert(command, IotDeviceCommandVo.class);
    }

    @Override public IotDeviceCommandVo getByRequestId(String requestId) {
        IotDeviceCommand command = commandMapper.selectOne(new LambdaQueryWrapper<IotDeviceCommand>().eq(IotDeviceCommand::getRequestId, requestId));
        if (command == null) throw new ServiceException("指令不存在");
        return MapstructUtils.convert(command, IotDeviceCommandVo.class);
    }

    @Override public TableDataInfo<IotDeviceCommandVo> selectPage(Long deviceId, PageQuery pageQuery) {
        Page<IotDeviceCommandVo> page = commandMapper.selectVoPage(pageQuery.build(), new LambdaQueryWrapper<IotDeviceCommand>()
            .eq(deviceId != null, IotDeviceCommand::getDeviceId, deviceId).orderByDesc(IotDeviceCommand::getCreateTime));
        return TableDataInfo.build(page);
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public void onAck(String deviceKey, String requestId, boolean accepted, String message) {
        IotDeviceCommand command = findCommand(deviceKey, requestId); if (command == null || isTerminal(command)) return;
        command.setAckTime(new Date());
        if (accepted) command.setCommandStatus("ACKED");
        else { command.setCommandStatus("FAILED"); command.setFailureReason(trim(message)); command.setFinishedTime(new Date()); }
        commandMapper.updateById(command);
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public void onState(String deviceKey, String requestId, String messageId, String lightState, String rawPayload) {
        IotDevice device = deviceMapper.selectOne(new LambdaQueryWrapper<IotDevice>().eq(IotDevice::getDeviceKey, deviceKey));
        if (device == null) return;
        Date now = new Date(); device.setOnlineStatus("1"); device.setLightState(lightState); device.setLastSeenTime(now); device.setLastReportTime(now); deviceMapper.updateById(device);
        if (!saveReport(device.getDeviceId(), requestId, messageId, "STATE", lightState, rawPayload)) return;
        if (requestId == null || requestId.isBlank()) return;
        IotDeviceCommand command = commandMapper.selectOne(new LambdaQueryWrapper<IotDeviceCommand>().eq(IotDeviceCommand::getRequestId, requestId).eq(IotDeviceCommand::getDeviceId, device.getDeviceId()));
        if (command != null && !isTerminal(command) && command.getDesiredLightState().equals(lightState)) {
            command.setCommandStatus("SUCCEEDED"); command.setFinishedTime(now); commandMapper.updateById(command);
        }
    }

    public void onPresence(String deviceKey, String messageId, boolean online, String reason, String rawPayload) {
        IotDevice device = deviceMapper.selectOne(new LambdaQueryWrapper<IotDevice>().eq(IotDevice::getDeviceKey, deviceKey));
        if (device == null) return;
        Date now = new Date(); device.setOnlineStatus(online ? "1" : "0"); device.setLastSeenTime(now);
        if (!online) device.setLastOfflineReason(trim(reason)); deviceMapper.updateById(device);
        saveReport(device.getDeviceId(), null, messageId, "PRESENCE", null, rawPayload);
    }

    private boolean saveReport(Long deviceId, String requestId, String messageId, String type, String lightState, String raw) {
        if (messageId == null || messageId.isBlank()) return false;
        if (reportMapper.selectCount(new LambdaQueryWrapper<IotDeviceReport>().eq(IotDeviceReport::getDeviceId, deviceId).eq(IotDeviceReport::getMessageId, messageId)) > 0) return false;
        IotDeviceReport report = new IotDeviceReport(); report.setDeviceId(deviceId); report.setRequestId(requestId); report.setReportType(type);
        report.setMessageId(messageId); report.setEventTime(new Date()); report.setReceivedTime(new Date()); report.setLightState(lightState); report.setPayload(raw); reportMapper.insert(report);
        return true;
    }
    private IotDeviceCommand findCommand(String deviceKey, String requestId) {
        IotDevice device = deviceMapper.selectOne(new LambdaQueryWrapper<IotDevice>().eq(IotDevice::getDeviceKey, deviceKey));
        return device == null ? null : commandMapper.selectOne(new LambdaQueryWrapper<IotDeviceCommand>().eq(IotDeviceCommand::getDeviceId, device.getDeviceId()).eq(IotDeviceCommand::getRequestId, requestId));
    }
    private boolean isTerminal(IotDeviceCommand command) { return "FAILED".equals(command.getCommandStatus()) || "SUCCEEDED".equals(command.getCommandStatus()) || "TIMEOUT".equals(command.getCommandStatus()); }
    private String trim(String value) { return value == null ? "设备未说明原因" : value.length() > 250 ? value.substring(0, 250) : value; }
    @Override @Scheduled(fixedDelay = 5000)
    public void markTimeoutCommands() {
        Date now = new Date();
        commandMapper.selectList(new LambdaQueryWrapper<IotDeviceCommand>().in(IotDeviceCommand::getCommandStatus, "PENDING", "PUBLISHED", "ACKED").lt(IotDeviceCommand::getTimeoutTime, now))
            .forEach(command -> { command.setCommandStatus("FAILED"); command.setFailureReason("设备执行超时"); command.setFinishedTime(now); commandMapper.updateById(command); });
    }

    /**
     * MQTT 遗嘱未送达时，按心跳阈值兜底将设备标记为离线。
     */
    @Scheduled(fixedDelay = 30000)
    public void markHeartbeatTimeoutDevices() {
        Date deadline = new Date(System.currentTimeMillis() - properties.getHeartbeatTimeoutSeconds() * 1000);
        deviceMapper.selectList(new LambdaQueryWrapper<IotDevice>().eq(IotDevice::getOnlineStatus, "1").lt(IotDevice::getLastSeenTime, deadline))
            .forEach(device -> { device.setOnlineStatus("0"); device.setLastOfflineReason("心跳超时"); deviceMapper.updateById(device); });
    }
}
