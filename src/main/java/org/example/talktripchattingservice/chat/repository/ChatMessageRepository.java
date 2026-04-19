package org.example.talktripchattingservice.chat.repository;

import org.example.talktripchattingservice.chat.entity.ChattingMessageHistory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChattingMessageHistory, String> {

    @Query(value = """
     SELECT IFNULL(SUM(not_read_message_count), 0) AS total_unread_message_count
     FROM (
         SELECT COUNT(*) AS not_read_message_count
         FROM chatting_room_account_tab crmt
         JOIN chatting_message_history_tab msg ON msg.room_id = crmt.room_id
         WHERE crmt.account_email = :userId
           AND (
               crmt.last_member_read_time IS NULL
               OR msg.created_at > crmt.last_member_read_time
           )
           AND msg.account_email != :userId
         GROUP BY crmt.room_id
     ) AS unread_counts;
   """, nativeQuery = true)
    int countUnreadMessages(@Param("userId") String userId);

    @Query(value = """
      SELECT COUNT(*)
      FROM chatting_message_history_tab cmht
      WHERE cmht.room_id = :roomId
        AND cmht.account_email != :memberId
        AND (
          (SELECT crat.last_member_read_time
           FROM chatting_room_account_tab crat
           WHERE crat.room_id = :roomId
             AND crat.account_email = :memberId
           LIMIT 1) IS NULL
          OR cmht.created_at > (SELECT crat2.last_member_read_time
                                FROM chatting_room_account_tab crat2
                                WHERE crat2.room_id = :roomId
                                  AND crat2.account_email = :memberId
                                LIMIT 1)
        )
    """, nativeQuery = true)
    int countUnreadMessagesByRoomIdAndMemberId(@Param("roomId") String roomId, @Param("memberId") String memberId);

    @Query(value = """
     select crat.account_email
           from chatting_room_account_tab crat
           where crat.room_id  = :roomId
           and crat.account_email  != :userId
           limit 1
   """, nativeQuery = true)
    String getOtherMemberIdByRoomIdandUserId(@Param("userId") String userId, @Param("roomId") String roomId);

    @Query("""
        select m
        from ChattingMessageHistory m
        where m.roomId = :roomId
        order by m.sequenceNumber desc, m.createdAt desc, m.messageId desc
    """)
    List<ChattingMessageHistory> findFirstPage(@Param("roomId") String roomId, PageRequest pageable);

    @Query("""
        select m
        from ChattingMessageHistory m
        where m.roomId = :roomId
          and m.sequenceNumber < :cursorSequenceNumber
        order by m.sequenceNumber desc, m.createdAt desc, m.messageId desc
    """)
    List<ChattingMessageHistory> findSliceBefore(@Param("roomId") String roomId, @Param("cursorSequenceNumber") Long cursorSequenceNumber, PageRequest pageable);

    @Query("""
        SELECT COALESCE(MAX(m.sequenceNumber), 0)
        FROM ChattingMessageHistory m
        WHERE m.roomId = :roomId
    """)
    Long findMaxSequenceNumberByRoomId(@Param("roomId") String roomId);

    @Query(value = """
            SELECT m.message
            FROM chatting_message_history_tab m
            WHERE m.room_id = :roomId
            ORDER BY m.sequence_number DESC, m.created_at DESC, m.message_id DESC
            LIMIT 1
            """, nativeQuery = true)
    List<String> findLatestMessageTextByRoomId(@Param("roomId") String roomId);
}

