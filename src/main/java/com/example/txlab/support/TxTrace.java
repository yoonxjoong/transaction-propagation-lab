package com.example.txlab.support;

import java.util.ArrayList;
import java.util.List;

/**
 * 테스트에서 "어느 시점에 어떤 물리 커넥션(pid)을 쓰고 있었는지"를 순서대로
 * 기록해서, 트랜잭션 전파 동작을 로그가 아니라 값으로 검증하기 위한 도구.
 */
public class TxTrace {

    private final List<String> events = new ArrayList<>();

    public void record(String label, int pid) {
        events.add(label + "=" + pid);
    }

    public List<String> events() {
        return events;
    }

    public Integer pidOf(String label) {
        for (String event : events) {
            String[] parts = event.split("=");
            if (parts[0].equals(label)) {
                return Integer.valueOf(parts[1]);
            }
        }
        throw new IllegalArgumentException("no such event recorded: " + label);
    }
}
