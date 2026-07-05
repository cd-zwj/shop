package com.payment.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PageResult - 统一分页返回结构")
class PageResultTest {

    // ============================================================
    // 1. from(Page) 正常转换
    // ============================================================
    @Nested
    @DisplayName("from(Page) 正常转换")
    class FromNormalPage {

        @Test
        @DisplayName("records 应正确复制")
        void records应正确复制() {
            // Arrange
            List<String> records = Arrays.asList("a", "b", "c");
            Page<String> page = new Page<>(1, 10, 3);
            page.setRecords(records);

            // Act
            PageResult<String> result = PageResult.from(page);

            // Assert
            assertThat(result.getRecords()).containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("total 应正确")
        void total应正确() {
            // Arrange
            Page<String> page = new Page<>(1, 10, 99);
            page.setRecords(Collections.emptyList());

            // Act
            PageResult<String> result = PageResult.from(page);

            // Assert
            assertThat(result.getTotal()).isEqualTo(99L);
        }

        @Test
        @DisplayName("page 应对应 current（当前页码）")
        void page应对应current() {
            // Arrange
            Page<String> page = new Page<>(5, 10, 100);
            page.setRecords(Collections.emptyList());

            // Act
            PageResult<String> result = PageResult.from(page);

            // Assert
            assertThat(result.getPage()).isEqualTo(5);
            assertThat(result.getCurrent()).isEqualTo(5);
        }

        @Test
        @DisplayName("size 应正确")
        void size应正确() {
            // Arrange
            Page<String> page = new Page<>(1, 20, 50);
            page.setRecords(Collections.emptyList());

            // Act
            PageResult<String> result = PageResult.from(page);

            // Assert
            assertThat(result.getSize()).isEqualTo(20);
            assertThat(result.getPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("空记录列表应返回空列表而非 null")
        void 空记录列表应返回空列表() {
            // Arrange
            Page<String> page = new Page<>(1, 10, 0);
            page.setRecords(Collections.emptyList());

            // Act
            PageResult<String> result = PageResult.from(page);

            // Assert
            assertThat(result.getRecords()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("records 为 null 时应返回空列表")
        void records为null时应返回空列表() {
            // Arrange
            Page<String> page = new Page<>(1, 10, 0);
            page.setRecords(null);

            // Act
            PageResult<String> result = PageResult.from(page);

            // Assert
            assertThat(result.getRecords()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("total 为 0 时应正确返回")
        void total为0时应正确返回() {
            // Arrange
            Page<String> page = new Page<>(1, 10, 0);
            page.setRecords(Collections.emptyList());

            // Act
            PageResult<String> result = PageResult.from(page);

            // Assert
            assertThat(result.getTotal()).isEqualTo(0L);
        }

        @Test
        @DisplayName("第一页的 page 应为 1")
        void 第一页page应为1() {
            // Arrange
            Page<String> page = new Page<>(1, 10, 5);
            page.setRecords(Arrays.asList("x"));

            // Act
            PageResult<String> result = PageResult.from(page);

            // Assert
            assertThat(result.getPage()).isEqualTo(1);
        }
    }

    // ============================================================
    // 2. from(null) 边界情况
    // ============================================================
    @Nested
    @DisplayName("from(null) 边界情况")
    class FromNull {

        @Test
        @DisplayName("传入 null 应返回空的默认 PageResult")
        void 传入null应返回默认空结果() {
            // Act
            PageResult<String> result = PageResult.from(null);

            // Assert
            assertThat(result.getRecords()).isNotNull().isEmpty();
            assertThat(result.getTotal()).isEqualTo(0L);
            assertThat(result.getPage()).isEqualTo(1);
            assertThat(result.getCurrent()).isEqualTo(1);
            assertThat(result.getSize()).isEqualTo(10);
            assertThat(result.getPages()).isEqualTo(0);
        }

        @Test
        @DisplayName("传入 null 返回的 records 不可变但不为 null")
        void 传入null返回的records不可变但不为null() {
            // Act
            PageResult<String> result = PageResult.from(null);

            // Assert
            assertThat(result.getRecords()).isNotNull();
            assertThat(result.getRecords()).isEmpty();
        }
    }

    // ============================================================
    // 3. 默认构造函数
    // ============================================================
    @Nested
    @DisplayName("默认构造函数")
    class DefaultConstructor {

        @Test
        @DisplayName("默认构造应返回空记录、0条总数、第1页、每页10条")
        void 默认构造应返回正确默认值() {
            // Act
            PageResult<String> result = new PageResult<>();

            // Assert
            assertThat(result.getRecords()).isNotNull().isEmpty();
            assertThat(result.getTotal()).isEqualTo(0L);
            assertThat(result.getPage()).isEqualTo(1);
            assertThat(result.getSize()).isEqualTo(10);
        }
    }

    // ============================================================
    // 4. 全参构造函数
    // ============================================================
    @Nested
    @DisplayName("全参构造函数")
    class ParameterizedConstructor {

        @Test
        @DisplayName("应正确设置所有字段")
        void 应正确设置所有字段() {
            // Arrange
            List<String> records = Arrays.asList("item1", "item2");

            // Act
            PageResult<String> result = new PageResult<>(records, 50L, 3, 15);

            // Assert
            assertThat(result.getRecords()).containsExactly("item1", "item2");
            assertThat(result.getTotal()).isEqualTo(50L);
            assertThat(result.getPage()).isEqualTo(3);
            assertThat(result.getCurrent()).isEqualTo(3);
            assertThat(result.getSize()).isEqualTo(15);
            assertThat(result.getPages()).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("兼容前端分页字段")
    class FrontendCompatibility {

        @Test
        @DisplayName("current 应始终等于 page")
        void current应等于page() {
            PageResult<String> result = new PageResult<>(List.of("a"), 21L, 2, 10);

            assertThat(result.getPage()).isEqualTo(2);
            assertThat(result.getCurrent()).isEqualTo(2);
        }

        @Test
        @DisplayName("pages 应按总数和页大小向上取整")
        void pages应向上取整() {
            PageResult<String> result = new PageResult<>(List.of("a"), 21L, 1, 10);

            assertThat(result.getPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("size 非正数时 pages 应为 0")
        void size非正数时pages为0() {
            PageResult<String> result = new PageResult<>(List.of(), 21L, 1, 0);

            assertThat(result.getPages()).isEqualTo(0);
        }
    }

    // ============================================================
    // 5. 大数据量场景
    // ============================================================
    @Nested
    @DisplayName("大数据量场景")
    class LargeDataSet {

        @Test
        @DisplayName("应正确处理大量记录")
        void 应正确处理大量记录() {
            // Arrange
            String[] bigArray = new String[10000];
            Arrays.fill(bigArray, "item");
            List<String> records = Arrays.asList(bigArray);
            Page<String> page = new Page<>(1, 10000, 10000);
            page.setRecords(records);

            // Act
            PageResult<String> result = PageResult.from(page);

            // Assert
            assertThat(result.getRecords()).hasSize(10000);
            assertThat(result.getTotal()).isEqualTo(10000L);
        }

        @Test
        @DisplayName("大页码应正确转换")
        void 大页码应正确转换() {
            // Arrange
            Page<String> page = new Page<>(9999, 50, 500000);
            page.setRecords(Collections.emptyList());

            // Act
            PageResult<String> result = PageResult.from(page);

            // Assert
            assertThat(result.getPage()).isEqualTo(9999);
            assertThat(result.getTotal()).isEqualTo(500000L);
            assertThat(result.getSize()).isEqualTo(50);
        }
    }
}
