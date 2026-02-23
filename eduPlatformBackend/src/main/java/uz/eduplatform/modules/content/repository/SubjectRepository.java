package uz.eduplatform.modules.content.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.eduplatform.modules.content.domain.Subject;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, UUID> {

    Page<Subject> findByUserIdAndIsArchivedFalse(UUID userId, Pageable pageable);

    Page<Subject> findByUserIdAndIsArchivedTrue(UUID userId, Pageable pageable);

    List<Subject> findByIsTemplateTrueAndIsActiveTrueOrderBySortOrderAsc();

    Optional<Subject> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT s FROM Subject s WHERE s.id = :id AND s.isArchived = false AND " +
           "(s.user.id = :userId OR s.isTemplate = true)")
    Optional<Subject> findAccessibleByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM subjects " +
            "WHERE user_id = :userId AND LOWER(name ->> 'uz_latn') = LOWER(:name) AND deleted_at IS NULL)",
            nativeQuery = true)
    boolean existsByUserIdAndDefaultName(@Param("userId") UUID userId, @Param("name") String name);

    @Query(value = "SELECT * FROM subjects " +
            "WHERE user_id = :userId AND LOWER(name ->> 'uz_latn') = LOWER(:name) AND deleted_at IS NULL LIMIT 1",
            nativeQuery = true)
    Optional<Subject> findByUserIdAndDefaultName(@Param("userId") UUID userId, @Param("name") String name);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM subjects WHERE user_id = :userId AND deleted_at IS NULL AND " +
            "(LOWER(name ->> 'uz_latn') = LOWER(:name) OR LOWER(name ->> 'uz_cyrl') = LOWER(:name) OR " +
            " LOWER(name ->> 'en') = LOWER(:name) OR LOWER(name ->> 'ru') = LOWER(:name)))",
            nativeQuery = true)
    boolean existsByUserIdAndNameInAnyLocale(@Param("userId") UUID userId, @Param("name") String name);

    @Query("SELECT s FROM Subject s WHERE s.isArchived = false AND " +
            "(s.user.id = :userId OR s.isTemplate = true) " +
            "ORDER BY s.sortOrder ASC")
    Page<Subject> findAllAccessibleByUser(@Param("userId") UUID userId, Pageable pageable);

    @Query(value = "SELECT s.* FROM subjects s WHERE s.is_archived = false AND s.deleted_at IS NULL AND " +
            "(s.user_id = :userId OR s.is_template = true) AND " +
            "(LOWER(s.name ->> 'uz_latn') LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(s.name ->> 'uz_cyrl') LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(s.name ->> 'en') LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(s.name ->> 'ru') LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(s.description ->> 'uz_latn') LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(s.description ->> 'uz_cyrl') LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(s.description ->> 'en') LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(s.description ->> 'ru') LIKE LOWER(CONCAT('%', :search, '%')))",
            countQuery = "SELECT COUNT(*) FROM subjects s WHERE s.is_archived = false AND s.deleted_at IS NULL AND " +
                    "(s.user_id = :userId OR s.is_template = true) AND " +
                    "(LOWER(s.name ->> 'uz_latn') LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    " LOWER(s.name ->> 'uz_cyrl') LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    " LOWER(s.name ->> 'en') LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    " LOWER(s.name ->> 'ru') LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    " LOWER(s.description ->> 'uz_latn') LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    " LOWER(s.description ->> 'uz_cyrl') LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    " LOWER(s.description ->> 'en') LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    " LOWER(s.description ->> 'ru') LIKE LOWER(CONCAT('%', :search, '%')))",
            nativeQuery = true)
    Page<Subject> searchByUser(@Param("userId") UUID userId, @Param("search") String search, Pageable pageable);

    long countByIsArchivedFalse();

    Page<Subject> findByIsArchivedFalseOrderByQuestionCountDesc(Pageable pageable);
}
