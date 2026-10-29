package org.ggy.iot.service;
import org.ggy.common.mybatis.core.page.PageQuery;
import org.ggy.common.mybatis.core.page.TableDataInfo;
import org.ggy.iot.domain.bo.IotDeviceBo;
import org.ggy.iot.domain.vo.IotDeviceCredentialVo;
import org.ggy.iot.domain.vo.IotDeviceVo;
/** IoT 设备服务。@ggy */
public interface IIotDeviceService {
    TableDataInfo<IotDeviceVo> selectPage(IotDeviceBo bo, PageQuery pageQuery);
    IotDeviceVo getById(Long deviceId);
    IotDeviceCredentialVo register(IotDeviceBo bo);
    void update(IotDeviceBo bo);
    IotDeviceCredentialVo rotateCredential(Long deviceId);
}
