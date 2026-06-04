package com.payment.controller;

import com.payment.dto.BillStatusVO;
import com.payment.util.JsonUtils;
import com.payment.common.Result;
import com.payment.dto.PaymentCallbackDTO;
import com.payment.entity.PaymentBill;
import com.payment.service.PaymentBillV1Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.util.Map;

@RestController
@RequestMapping("/v1/open/payments")
@RequiredArgsConstructor
public class V1OpenPaymentController {

    private final PaymentBillV1Service paymentBillV1Service;

    @PostMapping("/callbacks/{channelCode}")
    public Result<Void> handleCallback(@PathVariable String channelCode, @RequestBody PaymentCallbackDTO dto) {
        paymentBillV1Service.handleCallback(channelCode, dto);
        return Result.success();
    }

    @PostMapping("/callbacks/{channelCode}/recharge")
    public Result<Void> handleRechargeCallback(@PathVariable String channelCode, @RequestBody PaymentCallbackDTO dto) {
        paymentBillV1Service.handleCallback(channelCode, dto);
        return Result.success();
    }

    @PostMapping("/callbacks/{channelCode}/order")
    public Result<Void> handleOrderCallback(@PathVariable String channelCode, @RequestBody PaymentCallbackDTO dto) {
        paymentBillV1Service.handleCallback(channelCode, dto);
        return Result.success();
    }

    @PostMapping("/callbacks/alipay-page")
    public String handleAlipayPageCallback(@RequestParam Map<String, String> params) {
        PaymentCallbackDTO dto = new PaymentCallbackDTO();
        dto.setBillNo(params.get("out_trade_no"));
        dto.setCallbackRequestId(params.get("notify_id"));
        dto.setThirdPartyBillNo(params.get("trade_no"));
        dto.setSuccess("TRADE_SUCCESS".equals(params.get("trade_status"))
                || "TRADE_FINISHED".equals(params.get("trade_status")));
        dto.setRawBody(JsonUtils.toJson(params));
        paymentBillV1Service.handleCallback("ALIPAY_PAGE", dto);
        return "success";
    }

    @GetMapping("/bills/{billNo}/status")
    public Result<BillStatusVO> syncBillStatus(@PathVariable String billNo) {
        PaymentBill bill = paymentBillV1Service.syncBillStatus(billNo);
        BillStatusVO vo = new BillStatusVO();
        vo.setBillNo(bill.getBillNo());
        vo.setPayStatus(bill.getPayStatus());
        return Result.success(vo);
    }

    @GetMapping(value = "/returns/alipay-page", produces = MediaType.TEXT_HTML_VALUE)
    public String handleAlipayPageReturn(@RequestParam(required = false) String out_trade_no,
                                         @RequestParam(required = false) String trade_no) {
        return String.format("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>Alipay Payment Result</title>
                  <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; background: #f6f8fb; color: #1f2937; margin: 0; }
                    .card { max-width: 560px; margin: 64px auto; background: #fff; border-radius: 16px; padding: 32px; box-shadow: 0 12px 32px rgba(15, 23, 42, 0.08); }
                    h1 { margin: 0 0 12px; font-size: 24px; }
                    p { line-height: 1.7; margin: 8px 0; }
                    .meta { margin-top: 20px; padding: 16px; border-radius: 12px; background: #f8fafc; font-family: ui-monospace, SFMono-Regular, monospace; }
                  </style>
                </head>
                <body>
                  <div class="card">
                    <h1>Payment returned to merchant system</h1>
                    <p>The browser return only means the buyer completed the Alipay page flow. Final payment status must be confirmed by async notify or a bill status query.</p>
                    <div class="meta">
                      <div>billNo: %s</div>
                      <div>tradeNo: %s</div>
                    </div>
                  </div>
                </body>
                </html>
                """, safe(out_trade_no), safe(trade_no));
    }

    @GetMapping("/callbacks/ext-provider")
    public String handleExtProviderCallback(@RequestParam Map<String, String> params) {
        PaymentCallbackDTO dto = new PaymentCallbackDTO();
        dto.setBillNo(params.get("out_trade_no"));
        dto.setCallbackRequestId(params.get("trade_no"));
        dto.setThirdPartyBillNo(params.get("trade_no"));
        dto.setSuccess("TRADE_SUCCESS".equals(params.get("trade_status")));
        dto.setRawBody(JsonUtils.toJson(params));
        paymentBillV1Service.handleCallback("EXT_PROVIDER", dto);
        return "success";
    }

    private String safe(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value);
    }
}

