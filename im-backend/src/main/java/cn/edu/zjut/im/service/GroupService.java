package cn.edu.zjut.im.service;

import cn.edu.zjut.im.entity.Group;
import cn.edu.zjut.im.entity.GroupMember;
import cn.edu.zjut.im.repository.GroupMemberRepository;
import cn.edu.zjut.im.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserCacheService userCacheService;
    private final OnlineStatusService onlineStatusService;

    @Transactional
    public Group createGroup(String name, Long ownerId, List<Long> memberIds) {
        Group group = new Group();
        group.setName(name);
        group.setOwnerId(ownerId);
        group = groupRepository.save(group);

        GroupMember owner = new GroupMember();
        owner.setGroupId(group.getId());
        owner.setUserId(ownerId);
        owner.setRole("OWNER");
        groupMemberRepository.save(owner);

        if (memberIds != null) {
            for (Long memberId : memberIds) {
                if (!memberId.equals(ownerId) && !groupMemberRepository.existsByGroupIdAndUserId(group.getId(), memberId)) {
                    GroupMember member = new GroupMember();
                    member.setGroupId(group.getId());
                    member.setUserId(memberId);
                    member.setRole("MEMBER");
                    groupMemberRepository.save(member);
                }
            }
        }

        log.info("群组创建成功: id={}, name={}, ownerId={}, memberCount={}", group.getId(), name, ownerId, (memberIds != null ? memberIds.size() + 1 : 1));
        return group;
    }

    public String joinGroup(Long groupId, Long userId) {
        if (!groupRepository.existsById(groupId)) {
            return "群组不存在";
        }
        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            return "已在群中";
        }
        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole("MEMBER");
        groupMemberRepository.save(member);
        log.info("用户 {} 加入群组 {}", userId, groupId);
        return "加入成功";
    }

    @Transactional
    public String leaveGroup(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId).orElse(null);
        if (group == null) {
            return "群组不存在";
        }
        if (group.getOwnerId().equals(userId)) {
            return "群主不能退出，请先转让群主或解散群组";
        }
        groupMemberRepository.deleteByGroupIdAndUserId(groupId, userId);
        log.info("用户 {} 退出群组 {}", userId, groupId);
        return "已退出群组";
    }

    @Transactional
    public String dismissGroup(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId).orElse(null);
        if (group == null) {
            return "群组不存在";
        }
        if (!group.getOwnerId().equals(userId)) {
            return "只有群主可以解散群组";
        }
        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);
        groupMemberRepository.deleteAll(members);
        groupRepository.delete(group);
        log.info("群组 {} 被群主 {} 解散，共 {} 名成员", groupId, userId, members.size());
        return "群组已解散";
    }

    public Group findById(Long groupId) {
        return groupRepository.findById(groupId).orElse(null);
    }

    public List<Group> getUserGroups(Long userId) {
        List<GroupMember> memberships = groupMemberRepository.findByUserId(userId);
        List<Long> groupIds = memberships.stream().map(GroupMember::getGroupId).collect(Collectors.toList());
        return groupRepository.findAllById(groupIds);
    }

    public List<GroupMember> getGroupMembers(Long groupId) {
        return groupMemberRepository.findByGroupId(groupId);
    }

    public int getGroupMemberCount(Long groupId) {
        return groupMemberRepository.findByGroupId(groupId).size();
    }

    @Transactional
    public String kickMember(Long groupId, Long ownerId, Long targetUserId) {
        Group group = groupRepository.findById(groupId).orElse(null);
        if (group == null) {
            return "群组不存在";
        }
        if (!group.getOwnerId().equals(ownerId)) {
            return "只有群主可以踢人";
        }
        if (ownerId.equals(targetUserId)) {
            return "不能踢出自己";
        }
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, targetUserId)) {
            return "该用户不在群中";
        }
        groupMemberRepository.deleteByGroupIdAndUserId(groupId, targetUserId);
        log.info("群主 {} 将用户 {} 踢出群组 {}", ownerId, targetUserId, groupId);
        return "已踢出群组";
    }
}

