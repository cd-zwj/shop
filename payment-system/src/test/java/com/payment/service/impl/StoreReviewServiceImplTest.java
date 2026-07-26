package com.payment.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.payment.common.BusinessException;
import com.payment.dto.StoreReviewCreateDTO;
import com.payment.entity.SalesOrder;
import com.payment.entity.Store;
import com.payment.entity.StoreReview;
import com.payment.mapper.SalesOrderMapper;
import com.payment.mapper.StoreMapper;
import com.payment.mapper.StoreReviewMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoreReviewServiceImplTest {

    @BeforeAll
    static void initializeTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), SalesOrder.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), Store.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), StoreReview.class);
    }

    @Test
    void createShouldAcceptCompletedPickupOrderAndRefreshStoreRating() {
        Fixture fixture = new Fixture();
        SalesOrder order = completedOrder();
        StoreReviewCreateDTO dto = new StoreReviewCreateDTO();
        dto.setRating(5);
        dto.setContent("商品准备充分");
        dto.setImageUrls(List.of("https://cdn.example.com/review.jpg"));
        when(fixture.orderMapper.selectOne(any())).thenReturn(order);
        when(fixture.reviewMapper.selectCount(any())).thenReturn(0L);
        when(fixture.reviewMapper.selectList(any())).thenAnswer(invocation -> List.of(insertedReview(fixture)));
        when(fixture.storeMapper.selectById(7L)).thenReturn(store());

        StoreReview review = fixture.service.create(100L, 9L, "SO001", dto);

        assertEquals("VISIBLE", review.getStatus());
        assertEquals(5, review.getRating());
        verify(fixture.reviewMapper).insert(review);
        verify(fixture.storeMapper).updateById(any(Store.class));
    }

    @Test
    void createShouldRejectOrderThatIsNotCompleted() {
        Fixture fixture = new Fixture();
        SalesOrder order = completedOrder();
        order.setOrderStatus("PREPARING");
        StoreReviewCreateDTO dto = new StoreReviewCreateDTO();
        dto.setRating(5);
        when(fixture.orderMapper.selectOne(any())).thenReturn(order);

        assertThrows(BusinessException.class, () -> fixture.service.create(100L, 9L, "SO001", dto));

        verify(fixture.reviewMapper, never()).insert(any(StoreReview.class));
    }

    @Test
    void replyShouldRejectReviewFromAnotherTenant() {
        Fixture fixture = new Fixture();
        StoreReview review = insertedReview(fixture);
        review.setTenantId(10L);
        when(fixture.reviewMapper.selectById(1L)).thenReturn(review);

        assertThrows(BusinessException.class, () -> fixture.service.reply(9L, 1L, 200L, "谢谢反馈"));

        verify(fixture.reviewMapper, never()).updateById(any(StoreReview.class));
    }

    private static SalesOrder completedOrder() {
        SalesOrder order = new SalesOrder();
        order.setId(3L);
        order.setTenantId(9L);
        order.setPlatformUserId(100L);
        order.setStoreId(7L);
        order.setOrderNo("SO001");
        order.setOrderStatus("COMPLETED");
        order.setFulfillmentMode("STORE_PICKUP");
        order.setDeleted(0);
        return order;
    }

    private static Store store() {
        Store store = new Store();
        store.setId(7L);
        store.setTenantId(9L);
        return store;
    }

    private static StoreReview insertedReview(Fixture fixture) {
        return fixture.insertedReview == null ? new StoreReview() : fixture.insertedReview;
    }

    private static class Fixture {
        private final StoreReviewMapper reviewMapper = mock(StoreReviewMapper.class);
        private final SalesOrderMapper orderMapper = mock(SalesOrderMapper.class);
        private final StoreMapper storeMapper = mock(StoreMapper.class);
        private StoreReview insertedReview;
        private final StoreReviewServiceImpl service = new StoreReviewServiceImpl(reviewMapper, orderMapper, storeMapper);

        private Fixture() {
            org.mockito.Mockito.doAnswer(invocation -> {
                insertedReview = invocation.getArgument(0);
                return 1;
            }).when(reviewMapper).insert(any(StoreReview.class));
        }
    }
}
