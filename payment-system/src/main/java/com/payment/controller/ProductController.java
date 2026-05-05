package com.payment.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.payment.common.Result;
import com.payment.dto.ProductDTO;
import com.payment.entity.Product;
import com.payment.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 商品管理控制器
 */
 
 
@RestController
@RequestMapping("/product")
public class ProductController {
    
    @Autowired
    private ProductService productService;
    
    @Autowired(required = false)
    private com.payment.service.ProductSearchService productSearchService;
    

    @SaCheckPermission("product:create")
    @PostMapping("/create")
    public Result<Product> createProduct(@Valid @ModelAttribute ProductDTO dto) {
        Product product = productService.createProduct(dto);
        return Result.success(product);
    }

    @SaCheckPermission("product:update")
    @PutMapping("/update/{id}")
    public Result<Product> updateProduct(@PathVariable Long id, @ModelAttribute ProductDTO dto) {
        Product product = productService.updateProduct(id, dto);
        return Result.success(product);
    }

    @SaCheckPermission("product:delete")
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        productService.removeById(id);
        return Result.success();
    }

    @SaCheckPermission("product:view")
    @GetMapping("/detail/{id}")
    public Result<Product> getProduct(@PathVariable Long id) {
        // 使用带缓存的查询方法
        Product product = productService.getProductByIdWithCache(id);
        return Result.success(product);
    }

    @SaCheckPermission("product:view")
    @GetMapping("/code/{productCode}")
    public Result<Product> getByProductCode(@PathVariable String productCode) {
        Product product = productService.getByProductCode(productCode);
        return Result.success(product);
    }

    @SaCheckPermission("product:list")
    @GetMapping("/list")
    public Result<List<Product>> getProductList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        List<Product> list = productService.getProductList(keyword, category);
        return Result.success(list);
    }

    @SaCheckPermission("product:list")
    @GetMapping("/page")
    public Result<IPage<Product>> getProductPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        Page<Product> page = new Page<>(current, size);
        // 这里需要自定义分页查询，简化处理
        return Result.success(page);
    }

    @SaCheckPermission("product:search")
    @GetMapping("/search")
    public Result<List<Product>> searchProducts(@RequestParam String keyword) {
        if (productSearchService == null) {
            // 如果Elasticsearch未配置，降级到数据库查询
            List<Product> list = productService.getProductList(keyword, null);
            return Result.success(list);
        }
        List<Product> list = productSearchService.searchProducts(keyword, null);
        return Result.success(list);
    }

    @SaCheckPermission("product:search")
    @GetMapping("/search/category")
    public Result<List<Product>> searchByCategory(@RequestParam String category) {
        if (productSearchService == null) {
            // 如果Elasticsearch未配置，降级到数据库查询
            List<Product> list = productService.getProductList(null, category);
            return Result.success(list);
        }
        List<Product> list = productSearchService.searchByCategory(category);
        return Result.success(list);
    }
}

