package tr.com.allianz.ysv.services.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tr.com.allianz.ysv.services.entity.Municipality;

@Repository
public interface MunicipalityRepository extends JpaRepository<Municipality, Long> {

    Optional<Municipality> findByCityCodeAndDistrictCode(Integer cityCode, Integer districtCode);

    List<Municipality> findByCityCodeAndIsActive(Integer cityCode, String isActive);
}
