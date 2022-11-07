package com.argviewer.domain.interfaces.services;

import com.argviewer.domain.model.dtos.ProposicaoDTO;
import com.argviewer.domain.model.exceptions.IllegalOperationException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ProposicaoService {

    List<ProposicaoDTO> find(Integer usuarioId, Integer tagId);

    Optional<ProposicaoDTO> findById(int proposicaoId);

    Set<ProposicaoDTO> findByTextoContaining(String value);

    Set<ProposicaoDTO> findRespostas(int proposicaoId);

    int create(ProposicaoDTO dto);

    void update(ProposicaoDTO dto);

    boolean addResposta(int proposicaoId, int respostaId) throws IllegalOperationException;

    void addSeguidor(int proposicaoId, int seguidorId) throws IllegalOperationException;

    void removeSeguidor(int proposicaoId, int seguidorId) throws IllegalOperationException;

    void deleteById(int proposicaoId);
}
