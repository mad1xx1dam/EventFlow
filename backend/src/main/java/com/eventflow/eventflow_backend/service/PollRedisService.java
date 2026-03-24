package com.eventflow.eventflow_backend.service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PollRedisService {

    private final StringRedisTemplate stringRedisTemplate;

    public void initializeActivePoll(Long pollId, java.util.List<Long> optionIds) {
        String statusKey = buildStatusKey(pollId);
        String choicesKey = buildChoicesKey(pollId);
        String countsKey = buildCountsKey(pollId);

        stringRedisTemplate.opsForValue().set(statusKey, "ACTIVE");

        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
        for (Long optionId : optionIds) {
            hashOperations.put(countsKey, optionId.toString(), "0");
        }

        stringRedisTemplate.expire(statusKey, Duration.ofHours(12));
        stringRedisTemplate.expire(choicesKey, Duration.ofHours(12));
        stringRedisTemplate.expire(countsKey, Duration.ofHours(12));
    }

    public boolean isPollActive(Long pollId) {
        String status = stringRedisTemplate.opsForValue().get(buildStatusKey(pollId));
        return "ACTIVE".equals(status);
    }

    public void closePoll(Long pollId) {
        stringRedisTemplate.opsForValue().set(buildStatusKey(pollId), "CLOSED", Duration.ofHours(1));
    }

    public void saveVote(Long pollId, Long eventGuestId, Long pollOptionId) {
        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();

        String choicesKey = buildChoicesKey(pollId);
        String countsKey = buildCountsKey(pollId);
        String guestKey = eventGuestId.toString();
        String newOptionKey = pollOptionId.toString();

        String previousOptionKey = hashOperations.get(choicesKey, guestKey);

        if (previousOptionKey != null && previousOptionKey.equals(newOptionKey)) {
            return;
        }

        if (previousOptionKey != null) {
            hashOperations.increment(countsKey, previousOptionKey, -1);
        }

        hashOperations.put(choicesKey, guestKey, newOptionKey);
        hashOperations.increment(countsKey, newOptionKey, 1);
    }

    public Map<Long, Long> getCounts(Long pollId) {
        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
        Map<String, String> rawCounts = hashOperations.entries(buildCountsKey(pollId));

        Map<Long, Long> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : rawCounts.entrySet()) {
            result.put(Long.parseLong(entry.getKey()), Long.parseLong(entry.getValue()));
        }

        return result;
    }

    public Map<Long, Long> getChoices(Long pollId) {
        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
        Map<String, String> rawChoices = hashOperations.entries(buildChoicesKey(pollId));

        Map<Long, Long> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : rawChoices.entrySet()) {
            result.put(Long.parseLong(entry.getKey()), Long.parseLong(entry.getValue()));
        }

        return result;
    }

    public void cleanup(Long pollId) {
        stringRedisTemplate.delete(buildChoicesKey(pollId));
        stringRedisTemplate.delete(buildCountsKey(pollId));
        stringRedisTemplate.delete(buildStatusKey(pollId));
    }

    private String buildStatusKey(Long pollId) {
        return "poll:" + pollId + ":status";
    }

    private String buildChoicesKey(Long pollId) {
        return "poll:" + pollId + ":choices";
    }

    private String buildCountsKey(Long pollId) {
        return "poll:" + pollId + ":counts";
    }
}