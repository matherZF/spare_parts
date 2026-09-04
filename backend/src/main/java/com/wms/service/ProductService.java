package com.wms.service;

import com.wms.common.BizException;
import com.wms.dto.ProductDTO;
import com.wms.entity.Product;
import com.wms.repository.ProductRepository;
import com.wms.repository.PutawayOrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProductService {
    private final ProductRepository productRepo;
    private final PutawayOrderRepository putawayRepo;

    public ProductService(ProductRepository productRepo, PutawayOrderRepository putawayRepo) {
        this.productRepo = productRepo;
        this.putawayRepo = putawayRepo;
    }

    public Page<ProductDTO> page(String sku, String name, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        String s = StringUtils.hasText(sku) ? sku : "";
        String n = StringUtils.hasText(name) ? name : "";
        if (!s.isEmpty() || !n.isEmpty()) {
            return productRepo.findBySkuContainingOrNameContaining(s, n, pageable).map(this::toDTO);
        }
        return productRepo.findAll(pageable).map(this::toDTO);
    }

    public ProductDTO getById(Long id) {
        return productRepo.findById(id).map(this::toDTO)
                .orElseThrow(() -> new BizException("商品不存在"));
    }

    @Transactional
    public ProductDTO create(ProductDTO dto) {
        if (!StringUtils.hasText(dto.sku())) throw new BizException("sku不能为空");
        if (!StringUtils.hasText(dto.name())) throw new BizException("name不能为空");
        if (productRepo.existsBySku(dto.sku())) throw new BizException("sku已存在");
        Product p = new Product(dto.sku(), dto.name(), dto.spec(), dto.unit());
        productRepo.save(p);
        return toDTO(p);
    }

    @Transactional
    public ProductDTO update(Long id, ProductDTO dto) {
        Product p = productRepo.findById(id).orElseThrow(() -> new BizException("商品不存在"));
        p.setName(dto.name() == null ? p.getName() : dto.name());
        p.setSpec(dto.spec() == null ? p.getSpec() : dto.spec());
        p.setUnit(dto.unit() == null ? p.getUnit() : dto.unit());
        productRepo.save(p);
        return toDTO(p);
    }

    @Transactional
    public void delete(Long id) {
        if (!productRepo.existsById(id)) throw new BizException("商品不存在");
        if (putawayRepo.existsByProductId(id)) {
            throw new BizException("该商品已在单据中使用，无法删除");
        }
        productRepo.deleteById(id);
    }

    private ProductDTO toDTO(Product p) {
        return new ProductDTO(p.getId(), p.getSku(), p.getName(), p.getSpec(), p.getUnit(), p.getCreatedAt());
    }
}
