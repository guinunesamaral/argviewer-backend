package com.argviewer.services;

import com.argviewer.domain.interfaces.mappers.ProposicaoMapper;
import com.argviewer.domain.interfaces.mappers.UsuarioVoteMapper;
import com.argviewer.domain.interfaces.repositories.ProposicaoRepository;
import com.argviewer.domain.interfaces.repositories.UsuarioRepository;
import com.argviewer.domain.interfaces.repositories.UsuarioVoteRepository;
import com.argviewer.domain.interfaces.services.ProposicaoService;
import com.argviewer.domain.model.dtos.ProposicaoDTO;
import com.argviewer.domain.model.dtos.UsuarioVoteDTO;
import com.argviewer.domain.model.entities.Proposicao;
import com.argviewer.domain.model.entities.Usuario;
import com.argviewer.domain.model.entities.UsuarioVote;
import com.argviewer.domain.model.exceptions.EntityNotFoundException;
import com.argviewer.domain.model.exceptions.InvalidParameterException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.data.jpa.domain.Specification.where;

@Service
public class ProposicaoServiceImpl implements ProposicaoService {

    private final ProposicaoRepository proposicaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioVoteRepository usuarioVoteRepository;
    private final ProposicaoMapper proposicaoMapper;
    private final UsuarioVoteMapper usuarioVoteMapper;

    public ProposicaoServiceImpl(ProposicaoRepository proposicaoRepository, UsuarioRepository usuarioRepository, UsuarioVoteRepository usuarioVoteRepository, ProposicaoMapper proposicaoMapper, UsuarioVoteMapper usuarioVoteMapper) {
        this.proposicaoRepository = proposicaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioVoteRepository = usuarioVoteRepository;
        this.proposicaoMapper = proposicaoMapper;
        this.usuarioVoteMapper = usuarioVoteMapper;
    }

    static Specification<Proposicao> belongsTo(int usuarioId) {
        return (proposicao, cq, cb) -> cb.equal(proposicao.get("usuario").get("id"), usuarioId);
    }

    static Specification<Proposicao> containsTag(int tagId) {
        return (proposicao, cq, cb) -> proposicao.join("tags").get("id").in(Set.of(tagId));
    }

    @Override
    public List<ProposicaoDTO> find(Integer usuarioId, Integer tagId) {
        List<Proposicao> proposicoes;

        if (usuarioId != null && tagId != null)
            proposicoes = proposicaoRepository.findAll(
                    where(belongsTo(usuarioId)).and(containsTag(tagId)))
                    .stream()
                    .filter(Proposicao::isProposicaoInicial)
                    .collect(Collectors.toList());
        else if (usuarioId != null)
            proposicoes = proposicaoRepository.findAll(where(belongsTo(usuarioId)))
                    .stream()
                    .filter(Proposicao::isProposicaoInicial)
                    .collect(Collectors.toList());
        else if (tagId != null)
            proposicoes = proposicaoRepository.findAll(containsTag(tagId));
        else
            proposicoes = proposicaoRepository.findAll();

        return proposicaoMapper.proposicoesToDtoList(proposicoes
                .stream()
                .sorted((p1, p2) -> Integer.compare(p2.getRespostas().size(), p1.getRespostas().size()))
                .collect(Collectors.toList()));
    }

    @Override
    public Optional<ProposicaoDTO> findById(int id) {
        Optional<Proposicao> proposicao = proposicaoRepository.findById(id);
        return proposicao.map(proposicaoMapper::proposicaoToDto);
    }

    static Specification<Proposicao> containsTexto(String texto) {
        return (proposicao, cq, cb) -> proposicao.get("texto").in(texto);
    }

    @Override
    public List<ProposicaoDTO> findByTextoContaining(String value) {
        List<Proposicao> proposicoes = proposicaoRepository.findAll(where(containsTexto(value)));
        return proposicaoMapper.proposicoesToDtoList(proposicoes
                .stream()
                .sorted((p1, p2) -> Integer.compare(p2.getRespostas().size(), p1.getRespostas().size()))
                .collect(Collectors.toList()));
    }

    @Override
    public List<ProposicaoDTO> findRespostas(int proposicaoId) {
        Proposicao proposicao = proposicaoRepository
                .findById(proposicaoId)
                .orElseThrow(() -> new EntityNotFoundException("Proposição não encontrada."));

        return proposicaoMapper.proposicoesToDtoList(proposicao.getRespostas()
                .stream()
                .sorted((p1, p2) -> p2.getDataCriacao().compareTo(p1.getDataCriacao()))
                .collect(Collectors.toList()));
    }

    @Override
    public int create(ProposicaoDTO dto) throws InvalidParameterException {
        if (dto.getTexto().length() > 400)
            throw new InvalidParameterException("O texto da Proposição deve ter no máximo 400 caracteres");

        if (dto.getFonte().length() > 300)
            throw new InvalidParameterException("A fonte da Proposição deve ter no máximo 300 caracteres");

        if (usuarioRepository.findById(dto.getUsuario().getId()).isEmpty())
            throw new NullPointerException("O Usuário informado não foi encontrado.");

        Proposicao proposicao = proposicaoMapper.dtoToProposicao(dto);

        return proposicaoRepository.save(proposicao).getId();
    }

    @Override
    public void update(ProposicaoDTO dto) throws InvalidParameterException {
        if (dto.getTexto().length() > 400)
            throw new InvalidParameterException("O texto da Proposição deve ter no máximo 400 caracteres");

        if (dto.getFonte().length() > 300)
            throw new InvalidParameterException("A fonte da Proposição deve ter no máximo 300 caracteres");

        Proposicao proposicao = proposicaoRepository
                .findById(dto.getId())
                .orElseThrow(() -> new EntityNotFoundException("Proposição não encontrada."));

        proposicaoMapper.dtoToProposicao(dto, proposicao);
        proposicaoRepository.save(proposicao);
    }

    @Transactional
    @Override
    public void addResposta(int proposicaoId, ProposicaoDTO dto) {
        Proposicao proposicao = proposicaoRepository
                .findById(proposicaoId)
                .orElseThrow(() -> new EntityNotFoundException("Proposição não encontrada."));

        Proposicao resposta = proposicaoMapper.dtoToProposicao(dto);
        proposicaoRepository.save(resposta);

        proposicao.getRespostas().add(resposta);
        proposicaoRepository.save(proposicao);
    }

    @Override
    public void addVote(UsuarioVoteDTO dto) throws InvalidParameterException {
        Usuario usuario = usuarioRepository
                .findById(dto.getUsuario().getId())
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));

        Proposicao proposicao = proposicaoRepository
                .findById(dto.getProposicao().getId())
                .orElseThrow(() -> new EntityNotFoundException("Proposição não encontrada."));

        if (usuarioVoteRepository
                .findByUsuarioIdAndProposicaoId(usuario.getId(), proposicao.getId())
                .isPresent())
            throw new InvalidParameterException("Esse Usuário já votou nessa Proposicão.");

        if (dto.isUpvote())
            proposicao.setQtdUpvotes(proposicao.getQtdUpvotes() + 1);
        else
            proposicao.setQtdDownvotes(proposicao.getQtdDownvotes() + 1);

        proposicaoRepository.save(proposicao);

        UsuarioVote usuarioVote = usuarioVoteMapper.dtoToUsuarioVote(dto);
        usuarioVoteRepository.save(usuarioVote);
    }

    @Transactional
    @Override
    public void removeVote(UsuarioVoteDTO dto) throws InvalidParameterException {
        Usuario usuario = usuarioRepository
                .findById(dto.getUsuario().getId())
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));

        Proposicao proposicao = proposicaoRepository
                .findById(dto.getProposicao().getId())
                .orElseThrow(() -> new EntityNotFoundException("Proposição não encontrada."));

        Optional<UsuarioVote> usuarioVote = usuarioVoteRepository
                .findByUsuarioIdAndProposicaoId(usuario.getId(), proposicao.getId());

        if (usuarioVote.isEmpty())
            throw new InvalidParameterException("Esse Usuário não votou nessa Proposicão.");

        if (usuarioVote.get().isUpvote())
            proposicao.setQtdUpvotes(proposicao.getQtdUpvotes() - 1);
        else
            proposicao.setQtdDownvotes(proposicao.getQtdDownvotes() - 1);

        proposicaoRepository.save(proposicao);
        usuarioVoteRepository.deleteByUsuarioIdAndProposicaoId(usuario.getId(), proposicao.getId());
    }

    @Transactional
    @Override
    public void deleteById(int proposicaoId) {
        Proposicao proposicao = proposicaoRepository
                .findById(proposicaoId)
                .orElseThrow(() -> new EntityNotFoundException("Proposição não encontrada."));

        proposicaoRepository.deleteById(proposicaoId);
    }
}
