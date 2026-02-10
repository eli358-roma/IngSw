package com.hackhub.pattern.observer;

import com.hackhub.model.Hackathon;

public interface HackathonObserver {
    void onStatusChange(Hackathon hackathon, String oldStatus, String newStatus);
    void onJudgeAssigned(Hackathon hackathon);
    void onWinnerDeclared(Hackathon hackathon, Long winnerTeamId);
}