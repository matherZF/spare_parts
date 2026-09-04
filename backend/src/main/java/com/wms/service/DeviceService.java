package com.wms.service;

import com.wms.entity.Location;
import com.wms.repository.LocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 灯光提示设备服务（预留接口）
 * <p>
 * 当前为桩实现：仅打印日志，不实际操作硬件。
 * 后续接入灯光提示设备时，只需修改 lightUp / lightOff 方法的内部实现，
 * 例如通过串口/MQTT/HTTP 调用对应库位绑定的设备编号点亮/熄灭。
 */
@Service
public class DeviceService {
    private static final Logger log = LoggerFactory.getLogger(DeviceService.class);

    private final LocationRepository locationRepo;

    public DeviceService(LocationRepository locationRepo) {
        this.locationRepo = locationRepo;
    }

    /**
     * 点亮指定库位绑定的灯光设备
     *
     * @param locationId 库位 ID
     * @return 设备编号（未绑定则返回 null）
     */
    public String lightUp(Long locationId) {
        Location loc = locationRepo.findById(locationId).orElse(null);
        if (loc == null) {
            log.warn("[Device] 库位不存在: {}", locationId);
            return null;
        }
        String deviceNo = loc.getDeviceNo();
        if (deviceNo == null || deviceNo.isBlank()) {
            log.info("[Device] 库位 {} 未绑定灯光设备，跳过点亮", loc.getCode());
            return null;
        }
        // TODO: 后续在此处接入实际灯光设备驱动，例如：
        //   deviceClient.lightOn(deviceNo, "BLINK");
        log.info("[Device] 点亮库位 {} 的设备 {}", loc.getCode(), deviceNo);
        return deviceNo;
    }

    /**
     * 熄灭指定库位绑定的灯光设备
     */
    public void lightOff(Long locationId) {
        Location loc = locationRepo.findById(locationId).orElse(null);
        if (loc == null) return;
        String deviceNo = loc.getDeviceNo();
        if (deviceNo == null || deviceNo.isBlank()) return;
        // TODO: 接入实际设备驱动
        log.info("[Device] 熄灭库位 {} 的设备 {}", loc.getCode(), deviceNo);
    }
}
