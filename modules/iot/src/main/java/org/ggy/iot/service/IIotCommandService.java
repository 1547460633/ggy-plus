package org.ggy.iot.service;
import org.ggy.common.mybatis.core.page.PageQuery;
import org.ggy.common.mybatis.core.page.TableDataInfo;
import org.ggy.iot.domain.bo.IotLightCommandBo;
import org.ggy.iot.domain.vo.IotDeviceCommandVo;
/** IoT 指令服务。@ggy */
public interface IIotCommandService {
    IotDeviceCommandVo switchLight(Long deviceId, IotLightCommandBo bo);
    IotDeviceCommandVo getByRequestId(String requestId);
    TableDataInfo<IotDeviceCommandVo> selectPage(Long deviceId, PageQuery pageQuery);
    void onAck(String deviceKey, String requestId, boolean accepted, String message);
    void onState(String deviceKey, String requestId, String messageId, String lightState, String rawPayload);
    void markTimeoutCommands();
}
