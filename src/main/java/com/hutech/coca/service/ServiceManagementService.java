package com.hutech.coca.service;

import com.hutech.coca.repository.IBookingRepository;
import com.hutech.coca.repository.IServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServiceManagementService {
    
    private final IServiceRepository serviceRepository;
    private final IBookingRepository bookingRepository;
    
    public Page<com.hutech.coca.model.Service> getAllServices(String search, int pageNumber, int pageSize, 
                                                               String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("Ascending") 
                    ? Sort.by(sortBy).ascending() 
                    : Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, sort);
        return serviceRepository.findAllWithSearch(search, pageable);
    }
    
    public com.hutech.coca.model.Service getServiceById(Long id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ"));
    }
    
    @Transactional
    public com.hutech.coca.model.Service createService(com.hutech.coca.model.Service service) {
        // Validate tên dịch vụ trùng lặp
        if (serviceRepository.existsByNameIgnoreCase(service.getName())) {
            throw new IllegalArgumentException("Tên dịch vụ '" + service.getName() + "' đã tồn tại");
        }
        
        // Validate giá và thời gian
        if (service.getPrice() <= 0) {
            throw new IllegalArgumentException("Giá dịch vụ phải lớn hơn 0");
        }
        if (service.getDurationInMinutes() <= 0) {
            throw new IllegalArgumentException("Thời gian thực hiện phải lớn hơn 0");
        }
        
        return serviceRepository.save(service);
    }
    
    @Transactional
    public com.hutech.coca.model.Service updateService(com.hutech.coca.model.Service service) {
        com.hutech.coca.model.Service existing = getServiceById(service.getId());
        
        // Validate tên dịch vụ trùng lặp (loại trừ chính nó)
        if (serviceRepository.existsByNameIgnoreCaseAndIdNot(service.getName(), service.getId())) {
            throw new IllegalArgumentException("Tên dịch vụ '" + service.getName() + "' đã tồn tại");
        }
        
        // Validate giá và thời gian
        if (service.getPrice() <= 0) {
            throw new IllegalArgumentException("Giá dịch vụ phải lớn hơn 0");
        }
        if (service.getDurationInMinutes() <= 0) {
            throw new IllegalArgumentException("Thời gian thực hiện phải lớn hơn 0");
        }
        
        existing.setName(service.getName());
        existing.setDescription(service.getDescription());
        existing.setPrice(service.getPrice());
        existing.setDurationInMinutes(service.getDurationInMinutes());
        existing.setIsActive(service.getIsActive());
        existing.setImageUrl(service.getImageUrl());
        
        return serviceRepository.save(existing);
    }
    
    @Transactional
    public void toggleActive(Long id) {
        com.hutech.coca.model.Service service = getServiceById(id);
        // Xử lý NULL pointer: Nếu isActive = null, coi như false
        Boolean currentStatus = service.getIsActive();
        service.setIsActive(currentStatus != null && currentStatus ? false : true);
        serviceRepository.save(service);
    }
    
    @Transactional
    public void softDelete(Long id) {
        // Kiểm tra service có đang được sử dụng trong booking không
        long bookingCount = bookingRepository.countByServiceId(id);
        if (bookingCount > 0) {
            throw new IllegalStateException(
                "Không thể xóa dịch vụ đang có " + bookingCount + " booking sử dụng"
            );
        }
        
        // Soft delete sẽ được xử lý bởi @SQLDelete annotation
        serviceRepository.deleteById(id);
    }
}
