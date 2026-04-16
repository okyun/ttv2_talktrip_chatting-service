package org.example.talktripchattingservice.chat.repository;

import org.example.talktripchattingservice.chat.entity.ChatRoom;
import org.example.talktripchattingservice.chat.enums.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {

    @Modifying
    @Query("update ChatRoom r set r.updatedAt = :updatedAt where r.roomId = :roomId")
    int updateUpdatedAt(@Param("roomId") String roomId, @Param("updatedAt") LocalDateTime updatedAt);

    @Query("select r.roomType from ChatRoom r where r.roomId = :roomId")
    RoomType findRoomType(@Param("roomId") String roomId);

    @Query(value = """
        SELECT
            crt.room_id              AS roomId,
            crmt.room_account_id     AS roomAccountId,
            crt.created_at           AS createdAt,
            crt.updated_at           AS updatedAt,
            crt.title                AS title,
            (
                SELECT m.message
                FROM chatting_message_history_tab m
                WHERE m.room_id = crt.room_id
                ORDER BY m.sequence_number DESC, m.created_at DESC, m.message_id DESC
                LIMIT 1
            ) AS lastMessage,
            (
                SELECT COUNT(*)
                FROM chatting_message_history_tab m3
                WHERE m3.room_id = crt.room_id
                  AND (
                      crmt.last_member_read_time IS NULL
                      OR m3.created_at > crmt.last_member_read_time
                  )
                  AND m3.account_email <> :memberId
                  AND m3.account_email IS NOT NULL
            ) AS notReadMessageCount,
            crt.room_type AS roomType
        FROM chatting_room_account_tab crmt
        INNER JOIN chatting_room_tab crt ON crt.room_id = crmt.room_id
        WHERE crmt.account_email = :memberId
          AND crmt.is_del = 0
        ORDER BY crt.updated_at DESC
        """, nativeQuery = true)
    List<ChatRoomListRow> findRoomsWithLastMessageByMemberId(@Param("memberId") String memberId);
}

