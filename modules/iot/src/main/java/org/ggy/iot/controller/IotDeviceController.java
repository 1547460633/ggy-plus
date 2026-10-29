package org.ggy.iot.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckLogin;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ggy.common.core.domain.R;
import org.ggy.common.log.annotation.Log;
import org.ggy.common.log.enums.BusinessType;
import org.ggy.common.mybatis.core.page.PageQuery;
import org.ggy.common.mybatis.core.page.TableDataInfo;
import org.ggy.iot.domain.bo.IotDeviceBo;
import org.ggy.iot.domain.vo.IotDeviceCredentialVo;
import org.ggy.iot.domain.vo.IotDeviceVo;
import org.ggy.iot.service.IIotDeviceService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** IoT 设备管理接口。@ggy */
@Validated
@SaCheckLogin
@RestController
@RequiredArgsConstructor
@RequestMapping("/iot/devices")
public class IotDeviceController {
    private final IIotDeviceService deviceService;
    @SaCheckPermission("iot:device:list") @GetMapping("/list")
    public TableDataInfo<IotDeviceVo> list(IotDeviceBo bo, PageQuery pageQuery) { return deviceService.selectPage(bo, pageQuery); }
    @SaCheckPermission("iot:device:query") @GetMapping("/{deviceId}")
    public R<IotDeviceVo> get(@PathVariable Long deviceId) { return R.ok(deviceService.getById(deviceId)); }
    @Log(title = "IoT设备", businessType = BusinessType.INSERT) @SaCheckPermission("iot:device:add") @PostMapping
    public R<IotDeviceCredentialVo> add(@Valid @RequestBody IotDeviceBo bo) { return R.ok(deviceService.register(bo)); }
    @Log(title = "IoT设备", businessType = BusinessType.UPDATE) @SaCheckPermission("iot:device:edit") @PutMapping
    public R<Void> edit(@Valid @RequestBody IotDeviceBo bo) { deviceService.update(bo); return R.ok(); }
    @Log(title = "IoT设备凭证", businessType = BusinessType.UPDATE) @SaCheckPermission("iot:device:credential") @PostMapping("/{deviceId}/credential/rotate")
    public R<IotDeviceCredentialVo> rotate(@PathVariable Long deviceId) { return R.ok(deviceService.rotateCredential(deviceId)); }
}
