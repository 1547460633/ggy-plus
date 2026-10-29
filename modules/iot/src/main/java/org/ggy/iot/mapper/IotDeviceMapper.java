package org.ggy.iot.mapper;
import org.ggy.common.mybatis.core.mapper.BaseMapperPlus;
import org.ggy.iot.domain.IotDevice;
import org.ggy.iot.domain.vo.IotDeviceVo;
import org.apache.ibatis.annotations.Param;
/** IoT 设备 Mapper。@ggy */
public interface IotDeviceMapper extends BaseMapperPlus<IotDevice, IotDeviceVo> {
    /** 创建指令时锁定设备行，避免并发下发。 */
    IotDevice selectByIdForUpdate(@Param("deviceId") Long deviceId);
}
