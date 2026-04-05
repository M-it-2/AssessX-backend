package AssessX_backend.service;

import AssessX_backend.dto.ResultResponseDto;
import AssessX_backend.exception.ResultNotFoundException;
import AssessX_backend.repository.ResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResultService {

    private final ResultRepository resultRepository;

    public ResultService(ResultRepository resultRepository) {
        this.resultRepository = resultRepository;
    }

    @Transactional(readOnly = true)
    public List<ResultResponseDto> getMyResults(Long userId) {
        return resultRepository.findByUserId(userId).stream()
                .map(ResultResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ResultResponseDto> getGroupResults(Long groupId) {
        return resultRepository.findByGroupId(groupId).stream()
                .map(ResultResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResultResponseDto getResultById(Long id) {
        return resultRepository.findById(id)
                .map(ResultResponseDto::from)
                .orElseThrow(() -> new ResultNotFoundException(id));
    }
}
