package tr.com.allianz.ysv.services.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tr.com.allianz.ysv.services.entity.DeclarationLog;

@Repository
public interface DeclarationLogRepository extends JpaRepository<DeclarationLog, Long> {

    List<DeclarationLog> findByProcessIdOrderByIdDesc(Long processId);
}
