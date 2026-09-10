package com.saigonplantravel.backend.itinerary.repository;

import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class ItineraryItemSequenceRepository {

    private final EntityManager entityManager;

    public ItineraryItemSequenceRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public int decrementSequencesAfter(Long itineraryId, int deletedSequenceNo, OffsetDateTime updatedAt) {
        return entityManager
                .createNativeQuery(
                        """
                        UPDATE itinerary_items
                        SET sequence_no = sequence_no - 1,
                            updated_at = :updatedAt
                        WHERE itinerary_id = :itineraryId
                          AND sequence_no > :deletedSequenceNo
                        """)
                .setParameter("updatedAt", updatedAt)
                .setParameter("itineraryId", itineraryId)
                .setParameter("deletedSequenceNo", deletedSequenceNo)
                .executeUpdate();
    }

    public int shiftSequencesToTemporaryRange(Long itineraryId, int offset, OffsetDateTime updatedAt) {
        return entityManager
                .createNativeQuery(
                        """
                        UPDATE itinerary_items
                        SET sequence_no = sequence_no + :offset,
                            updated_at = :updatedAt
                        WHERE itinerary_id = :itineraryId
                        """)
                .setParameter("offset", offset)
                .setParameter("updatedAt", updatedAt)
                .setParameter("itineraryId", itineraryId)
                .executeUpdate();
    }

    public int applyOrder(Long itineraryId, List<UUID> orderedItemPublicIds, OffsetDateTime updatedAt) {
        StringBuilder sql = new StringBuilder("UPDATE itinerary_items SET sequence_no = CASE public_id ");

        for (int index = 0; index < orderedItemPublicIds.size(); index++) {
            sql.append("WHEN :publicId")
                    .append(index)
                    .append(" THEN :sequence")
                    .append(index)
                    .append(' ');
        }

        sql.append("END, updated_at = :updatedAt WHERE itinerary_id = :itineraryId");

        var query = entityManager
                .createNativeQuery(sql.toString())
                .setParameter("updatedAt", updatedAt)
                .setParameter("itineraryId", itineraryId);

        for (int index = 0; index < orderedItemPublicIds.size(); index++) {
            query.setParameter("publicId" + index, orderedItemPublicIds.get(index));
            query.setParameter("sequence" + index, index + 1);
        }

        return query.executeUpdate();
    }
}
