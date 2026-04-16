package org.example.talktripchattingservice.chat.repository;

import org.example.talktripchattingservice.chat.entity.ChatRoomAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomAccount, String> {

    boolean existsByRoomIdAndAccountEmail(String roomId, String accountEmail);

    @Query("select c from ChatRoomAccount c where c.roomId = :roomId and c.isDel = 0")
    List<ChatRoomAccount> findAllAccountEmailsByRoomId(@Param("roomId") String roomId);

    @Modifying
    @Query("update ChatRoomAccount crm set crm.lastMemberReadTime = :currentTime where crm.roomId = :roomId and crm.accountEmail = :accountEmail")
    int updateLastReadTime(@Param("roomId") String roomId, @Param("accountEmail") String accountEmail, @Param("currentTime") LocalDateTime currentTime);

    @Modifying
    @Query("update ChatRoomAccount crm set crm.isDel = :isDel where crm.roomId = :roomId and crm.accountEmail = :accountEmail")
    int updateIsDelByMemberIdAndRoomId(@Param("accountEmail") String accountEmail, @Param("roomId") String roomId, @Param("isDel") int isDel);

    @Modifying
    @Query("update ChatRoomAccount crm set crm.isDel = 0 where crm.roomId = :roomId")
    int resetIsDelByRoomId(@Param("roomId") String roomId);

    @Query("""
        select distinct crm.roomId
        from ChatRoomAccount crm
        where crm.accountEmail in (:buyerEmail, :sellerEmail)
          and crm.isDel = 0
        group by crm.roomId
        having count(distinct crm.accountEmail) = 2
    """)
    Optional<String> findRoomIdByBuyerIdAndSellerId(@Param("buyerEmail") String buyerEmail, @Param("sellerEmail") String sellerEmail);
}

