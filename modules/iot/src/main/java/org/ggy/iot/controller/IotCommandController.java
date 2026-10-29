package org.ggy.iot.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckLogin;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ggy.common.core.domain.R;
import org.ggy.common.log.annotation.Log;
import org.ggy.common.log.enums.BusinessType;
import org.ggy.common.idempotent.annotation.RepeatSubmit;
import org.ggy.common.mybatis.core.page.PageQuery;
import org.ggy.common.mybatis.core.page.TableDataInfo;
import org.ggy.iot.domain.bo.IotLightCommandBo;
import org.ggy.iot.domain.vo.IotDeviceCommandVo;
import org.ggy.iot.service.IIotCommandService;
import org.springframework.web.bind.annotation.*;

/** IoT 灯控和指令查询接口。@ggy */
@RestController
@SaCheckLogin
@RequiredArgsConstructor
@RequestMapping("/iot")
public class IotCommandController {
    private final IIotCommandService commandService;
    @Log(title = "IoT远程点灯", businessType = BusinessType.UPDATE) @SaCheckPermission("iot:device:control") @PostMapping("/devices/{deviceId}/light")
    @RepeatSubmit
    public R<IotDeviceCommandVo> switchLight(@PathVariable Long deviceId, @Valid @RequestBody IotLightCommandBo bo) { return R.ok(commandService.switchLight(deviceId, bo)); }
    @SaCheckPermission("iot:command:list") @GetMapping("/commands/list")
    public TableDataInfo<IotDeviceCommandVo> list(Long deviceId, PageQuery pageQuery) { return commandService.selectPage(deviceId, pageQuery); }
    @SaCheckPermission("iot:command:query") @GetMapping("/commands/{requestId}")
    public R<IotDeviceCommandVo> get(@PathVariable String requestId) { return R.ok(commandService.getByRequestId(requestId)); }
}
