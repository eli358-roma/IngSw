package com.hackhub.pattern.observer;

import com.hackhub.model.Hackathon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class HackathonObservable {

    private final List<HackathonObserver> observers = new ArrayList<>();

    @Autowired
    public HackathonObservable(List<HackathonObserver> observerList) {
        this.observers.addAll(observerList);
    }

    public void addObserver(HackathonObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(HackathonObserver observer) {
        observers.remove(observer);
    }

    public void notifyStatusChange(Hackathon hackathon, String oldStatus, String newStatus) {
        observers.forEach(observer ->
                observer.onStatusChange(hackathon, oldStatus, newStatus)
        );
    }

    public void notifyJudgeAssigned(Hackathon hackathon) {
        observers.forEach(observer ->
                observer.onJudgeAssigned(hackathon)
        );
    }

    public void notifyWinnerDeclared(Hackathon hackathon, Long winnerTeamId) {
        observers.forEach(observer ->
                observer.onWinnerDeclared(hackathon, winnerTeamId)
        );
    }
}