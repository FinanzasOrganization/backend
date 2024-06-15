package com.wheelcredit.Backend.repository;

import com.wheelcredit.Backend.model.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token,Long> {
    @Query(value = """
            select t from Token t inner join Client c\s
            on t.client.id = c.id\s
            where c.id = :id and (t.expired = false or t.revoked = false)\s
            """)
    List<Token> findAllValidTokenByClient(Long id);

    Optional<Token> findByToken(String token);
}
