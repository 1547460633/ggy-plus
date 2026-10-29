package org.ggy.iot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.ggy.common.core.exception.ServiceException;
import org.ggy.common.core.utils.MapstructUtils;
import org.ggy.common.mybatis.core.page.PageQuery;
import org.ggy.common.mybatis.core.page.TableDataInfo;
import org.ggy.iot.domain.IotDevice;
import org.ggy.iot.domain.bo.IotDeviceBo;
import org.ggy.iot.domain.vo.IotDeviceCredentialVo;
import org.ggy.iot.domain.vo.IotDeviceVo;
import org.ggy.iot.mapper.IotDeviceMapper;
import org.ggy.iot.service.IIotDeviceService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/** IoT 设备服务实现。@ggy */
@Service
@RequiredArgsConstructor
public class IotDeviceServiceImpl implements IIotDeviceService {
    private static final BCryptPasswordEncoder SECRET_ENCODER = new BCryptPasswordEncoder();
    private static final SecureRandom RANDOM = new SecureRandom();
    private final IotDeviceMapper deviceMapper;

    @Override
    public TableDataInfo<IotDeviceVo> selectPage(IotDeviceBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<IotDevice> wrapper = new LambdaQueryWrapper<IotDevice>()
            .like(bo.getDeviceName() != null && !bo.getDeviceName().isBlank(), IotDevice::getDeviceName, bo.getDeviceName())
            .eq(bo.getEnabled() != null && !bo.getEnabled().isBlank(), IotDevice::getEnabled, bo.getEnabled())
            .orderByDesc(IotDevice::getCreateTime);
        Page<IotDeviceVo> page = deviceMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public IotDeviceVo getById(Long deviceId) {
        IotDeviceVo result = deviceMapper.selectVoById(deviceId);
        if (result == null) throw new ServiceException("设备不存在");
        return result;
    }

    @Override
    public IotDeviceCredentialVo register(IotDeviceBo bo) {
        IotDevice device = MapstructUtils.convert(bo, IotDevice.class);
        String secret = newSecret();
        device.setDeviceKey("esp32-" + UUID.randomUUID().toString().replace("-", ""));
        device.setDeviceSecretHash(SECRET_ENCODER.encode(secret));
        device.setOnlineStatus("0"); device.setLightState("U");
        deviceMapper.insert(device);
        return new IotDeviceCredentialVo(device.getDeviceId(), device.getDeviceKey(), secret);
    }

    @Override
    public void update(IotDeviceBo bo) {
        if (bo.getDeviceId() == null) throw new ServiceException("设备ID不能为空");
        IotDevice device = MapstructUtils.convert(bo, IotDevice.class);
        if (deviceMapper.updateById(device) == 0) throw new ServiceException("设备不存在");
    }

    @Override
    public IotDeviceCredentialVo rotateCredential(Long deviceId) {
        IotDevice device = deviceMapper.selectById(deviceId);
        if (device == null) throw new ServiceException("设备不存在");
        String secret = newSecret(); device.setDeviceSecretHash(SECRET_ENCODER.encode(secret));
        deviceMapper.updateById(device);
        return new IotDeviceCredentialVo(device.getDeviceId(), device.getDeviceKey(), secret);
    }

    private String newSecret() { byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
}
