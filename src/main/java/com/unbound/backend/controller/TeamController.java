package com.unbound.backend.controller;

import com.unbound.backend.entity.*;
import com.unbound.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student/teams")
public class TeamController {
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private TeamMembersRepository teamMembersRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private EventRepository eventRepository;

    private Student getStudentForUser(User user) {
        return studentRepository.findAll().stream()
                .filter(s -> s.getUser().getUid().equals(user.getUid()))
                .findFirst().orElse(null);
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<?> viewTeamsForEvent(@PathVariable Integer eventId) {
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null) return ResponseEntity.status(404).body(Map.of("error", "Event not found"));
        List<Team> teams = teamRepository.findByEvent(event);
        return ResponseEntity.ok(teams);
    }

    @GetMapping("/my")
    public ResponseEntity<?> myTeams(@AuthenticationPrincipal User user) {
        if (user == null || user.getRole() != User.Role.Student) {
            return ResponseEntity.status(403).body(Map.of("error", "Only students can view their teams"));
        }
        Student student = getStudentForUser(user);
        if (student == null) return ResponseEntity.status(404).body(Map.of("error", "Student not found"));
        List<TeamMembers> memberships = teamMembersRepository.findByStudent(student);
        List<Team> teams = memberships.stream().map(TeamMembers::getTeam).collect(Collectors.toList());
        return ResponseEntity.ok(teams);
    }

    @GetMapping("/{teamId}/members")
    public ResponseEntity<?> viewTeamMembers(@PathVariable Integer teamId) {
        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null) return ResponseEntity.status(404).body(Map.of("error", "Team not found"));
        List<TeamMembers> members = teamMembersRepository.findByTeam(team);
        List<Student> students = members.stream().map(TeamMembers::getStudent).collect(Collectors.toList());
        return ResponseEntity.ok(students);
    }

    @DeleteMapping("/{teamId}/leave")
    public ResponseEntity<?> leaveTeam(@AuthenticationPrincipal User user, @PathVariable Integer teamId) {
        if (user == null || user.getRole() != User.Role.Student) {
            return ResponseEntity.status(403).body(Map.of("error", "Only students can leave teams"));
        }
        Student student = getStudentForUser(user);
        if (student == null) return ResponseEntity.status(404).body(Map.of("error", "Student not found"));
        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null) return ResponseEntity.status(404).body(Map.of("error", "Team not found"));
        TeamMembers.TeamMemberId id = new TeamMembers.TeamMemberId(team.getTid(), student.getSid());
        if (!teamMembersRepository.existsById(id)) {
            return ResponseEntity.badRequest().body(Map.of("error", "You are not a member of this team"));
        }
        teamMembersRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Left the team successfully"));
    }
} 