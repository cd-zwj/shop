package com.payment.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class OrderDeliveryRecordMapperMaintenanceTest {

    @Test
    void rotationQueriesShouldBypassTenantInterceptorAndIncludePhysicalRows() throws Exception {
        Method select = OrderDeliveryRecordMapper.class.getMethod(
                "selectPickupCodeRotationBatch", long.class, int.class);
        Method update = OrderDeliveryRecordMapper.class.getMethod(
                "compareAndSetPickupCodePayload", Long.class, String.class, String.class);

        assertThat(select.getAnnotation(InterceptorIgnore.class).tenantLine()).isEqualTo("true");
        assertThat(update.getAnnotation(InterceptorIgnore.class).tenantLine()).isEqualTo("true");
        assertThat(String.join(" ", select.getAnnotation(Select.class).value()))
                .doesNotContainIgnoringCase("deleted")
                .contains("id > #{cursor}", "LIMIT #{batchSize}");
        assertThat(String.join(" ", update.getAnnotation(Update.class).value()))
                .contains("id = #{id}", "payload = #{oldPayload}");
    }
}
