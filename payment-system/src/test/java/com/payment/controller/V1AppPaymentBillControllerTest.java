package com.payment.controller;

import com.payment.common.BusinessException;
import com.payment.entity.PaymentBill;
import com.payment.service.PaymentBillV1Service;
import com.payment.util.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("V1AppPaymentBillController - 水平越权防护")
class V1AppPaymentBillControllerTest {

    private PaymentBillV1Service paymentBillV1Service;
    private V1AppPaymentBillController controller;
    private MockedStatic<UserContext> userContextMock;

    @BeforeEach
    void setUp() {
        paymentBillV1Service = mock(PaymentBillV1Service.class);
        controller = new V1AppPaymentBillController(paymentBillV1Service);
        // mock UserContext 静态方法，默认返回当前用户 ID = 100
        userContextMock = mockStatic(UserContext.class);
        userContextMock.when(UserContext::getCurrentUserId).thenReturn(100L);
    }

    @AfterEach
    void tearDown() {
        if (userContextMock != null) {
            userContextMock.close();
        }
    }

    // ============================================================
    // 辅助方法
    // ============================================================
    private PaymentBill buildBill(String billNo, Long platformUserId) {
        PaymentBill bill = new PaymentBill();
        bill.setBillNo(billNo);
        bill.setPlatformUserId(platformUserId);
        bill.setPayStatus("SUCCESS");
        return bill;
    }

    // ============================================================
    // 1. getPaymentBill
    // ============================================================
    @Nested
    @DisplayName("getPaymentBill - 查询支付单")
    class GetPaymentBill {

        @Test
        @DisplayName("查询自己的支付单应正常返回")
        void 查询自己的支付单应正常返回() {
            // Arrange
            PaymentBill myBill = buildBill("BILL-001", 100L);
            when(paymentBillV1Service.getByBillNo("BILL-001")).thenReturn(myBill);

            // Act & Assert
            assertThatCode(() -> controller.getPaymentBill("BILL-001"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("查询自己的支付单返回的 data 应为对应 bill")
        void 查询自己的支付单应返回正确bill() {
            // Arrange
            PaymentBill myBill = buildBill("BILL-001", 100L);
            when(paymentBillV1Service.getByBillNo("BILL-001")).thenReturn(myBill);

            // Act
            var result = controller.getPaymentBill("BILL-001");

            // Assert
            assertThat(result.getData()).isNotNull();
            assertThat(result.getData().getBillNo()).isEqualTo("BILL-001");
        }

        @Test
        @DisplayName("查询别人的支付单应抛出 BusinessException")
        void 查询别人的支付单应抛出异常() {
            // Arrange
            PaymentBill otherBill = buildBill("BILL-002", 200L);
            when(paymentBillV1Service.getByBillNo("BILL-002")).thenReturn(otherBill);

            // Act & Assert
            assertThatThrownBy(() -> controller.getPaymentBill("BILL-002"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("查询别人的支付单异常 code 应为 403")
        void 查询别人的支付单异常code应为403() {
            // Arrange
            PaymentBill otherBill = buildBill("BILL-002", 200L);
            when(paymentBillV1Service.getByBillNo("BILL-002")).thenReturn(otherBill);

            // Act & Assert
            assertThatThrownBy(() -> controller.getPaymentBill("BILL-002"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException bex = (BusinessException) ex;
                        assertThat(bex.getCode()).isEqualTo(403);
                    });
        }

        @Test
        @DisplayName("查询别人的支付单异常 message 应为'无权访问该支付单'")
        void 查询别人的支付单异常message应为无权访问该支付单() {
            // Arrange
            PaymentBill otherBill = buildBill("BILL-002", 200L);
            when(paymentBillV1Service.getByBillNo("BILL-002")).thenReturn(otherBill);

            // Act & Assert
            assertThatThrownBy(() -> controller.getPaymentBill("BILL-002"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("无权访问该支付单");
        }

        @Test
        @DisplayName("支付单不存在应抛出 BusinessException")
        void 支付单不存在应抛出异常() {
            // Arrange
            when(paymentBillV1Service.getByBillNo("NOT_EXIST")).thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> controller.getPaymentBill("NOT_EXIST"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("支付单不存在");
        }

        @Test
        @DisplayName("platformUserId 为 null 时不触发越权校验")
        void platformUserId为null时不触发越权校验() {
            // Arrange
            PaymentBill bill = buildBill("BILL-003", null);
            when(paymentBillV1Service.getByBillNo("BILL-003")).thenReturn(bill);

            // Act & Assert
            assertThatCode(() -> controller.getPaymentBill("BILL-003"))
                    .doesNotThrowAnyException();
        }
    }

    // ============================================================
    // 2. getLatestPaymentBillByBiz - 按业务单号查询最近支付单
    // ============================================================
    @Nested
    @DisplayName("getLatestPaymentBillByBiz - 按业务单号查询最近支付单")
    class GetLatestPaymentBillByBiz {

        @Test
        @DisplayName("查询自己的充值业务支付单应正常返回")
        void 查询自己的充值业务支付单应正常返回() {
            // Arrange
            PaymentBill myBill = buildBill("BILL-RECHARGE-001", 100L);
            myBill.setBizType("RECHARGE");
            myBill.setBizNo("WR202607060001");
            when(paymentBillV1Service.getLatestByBizTypeAndBizNo("RECHARGE", "WR202607060001"))
                    .thenReturn(myBill);

            // Act
            var result = controller.getLatestPaymentBillByBiz("RECHARGE", "WR202607060001");

            // Assert
            assertThat(result.getData()).isNotNull();
            assertThat(result.getData().getBillNo()).isEqualTo("BILL-RECHARGE-001");
            assertThat(result.getData().getBizNo()).isEqualTo("WR202607060001");
            verify(paymentBillV1Service).getLatestByBizTypeAndBizNo("RECHARGE", "WR202607060001");
        }

        @Test
        @DisplayName("业务单号为空应抛出 BusinessException")
        void 业务单号为空应抛出异常() {
            assertThatThrownBy(() -> controller.getLatestPaymentBillByBiz("RECHARGE", " "))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("业务类型和业务单号不能为空");
        }

        @Test
        @DisplayName("查询别人的业务支付单应抛出 BusinessException")
        void 查询别人的业务支付单应抛出异常() {
            // Arrange
            PaymentBill otherBill = buildBill("BILL-RECHARGE-002", 200L);
            when(paymentBillV1Service.getLatestByBizTypeAndBizNo("RECHARGE", "WR202607060002"))
                    .thenReturn(otherBill);

            // Act & Assert
            assertThatThrownBy(() -> controller.getLatestPaymentBillByBiz("RECHARGE", "WR202607060002"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("无权访问该支付单");
        }

        @Test
        @DisplayName("业务支付单不存在应抛出 BusinessException")
        void 业务支付单不存在应抛出异常() {
            // Arrange
            when(paymentBillV1Service.getLatestByBizTypeAndBizNo("RECHARGE", "WR-NOT-FOUND"))
                    .thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> controller.getLatestPaymentBillByBiz("RECHARGE", "WR-NOT-FOUND"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("支付单不存在");
        }
    }

    // ============================================================
    // 3. syncPaymentBill
    // ============================================================
    @Nested
    @DisplayName("syncPaymentBill - 同步支付单状态")
    class SyncPaymentBill {

        @Test
        @DisplayName("同步自己的支付单应正常返回")
        void 同步自己的支付单应正常返回() {
            // Arrange
            PaymentBill myBill = buildBill("BILL-100", 100L);
            when(paymentBillV1Service.getByBillNo("BILL-100")).thenReturn(myBill);
            when(paymentBillV1Service.syncBillStatus("BILL-100")).thenReturn(myBill);

            // Act & Assert
            assertThatCode(() -> controller.syncPaymentBill("BILL-100"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("同步别人的支付单应抛出 BusinessException")
        void 同步别人的支付单应抛出异常() {
            // Arrange
            PaymentBill otherBill = buildBill("BILL-200", 200L);
            when(paymentBillV1Service.getByBillNo("BILL-200")).thenReturn(otherBill);

            // Act & Assert
            assertThatThrownBy(() -> controller.syncPaymentBill("BILL-200"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("无权访问该支付单");
        }

        @Test
        @DisplayName("同步不存在的支付单应抛出 BusinessException")
        void 同步不存在的支付单应抛出异常() {
            // Arrange
            when(paymentBillV1Service.getByBillNo("NOT_EXIST")).thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> controller.syncPaymentBill("NOT_EXIST"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("支付单不存在");
        }
    }

    // ============================================================
    // 4. 不同用户场景
    // ============================================================
    @Nested
    @DisplayName("不同用户 ID 场景")
    class DifferentUserScenarios {

        @Test
        @DisplayName("当前用户 ID 与 bill 相同时不抛异常")
        void 相同用户ID不抛异常() {
            // Arrange
            PaymentBill bill = buildBill("B-1", 100L);
            when(paymentBillV1Service.getByBillNo("B-1")).thenReturn(bill);

            // Act & Assert
            assertThatCode(() -> controller.getPaymentBill("B-1"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("当前用户 ID 与 bill 不同时抛异常")
        void 不同用户ID应抛异常() {
            // Arrange
            PaymentBill bill = buildBill("B-2", 999L);
            when(paymentBillV1Service.getByBillNo("B-2")).thenReturn(bill);

            // Act & Assert
            assertThatThrownBy(() -> controller.getPaymentBill("B-2"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("当 UserContext 返回不同 userId 时，同一 bill 从可访问变为不可访问")
        void 更换用户后同一bill变为不可访问() {
            // Arrange: 先以用户 100 访问
            PaymentBill bill = buildBill("B-SWITCH", 100L);
            when(paymentBillV1Service.getByBillNo("B-SWITCH")).thenReturn(bill);

            assertThatCode(() -> controller.getPaymentBill("B-SWITCH"))
                    .doesNotThrowAnyException();

            // Act: 切换当前用户为 200
            userContextMock.when(UserContext::getCurrentUserId).thenReturn(200L);

            // Assert: 此时不应访问
            assertThatThrownBy(() -> controller.getPaymentBill("B-SWITCH"))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
