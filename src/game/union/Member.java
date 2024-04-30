package game.union;

// 帮会成员
public class Member {
    long id; // 成员名
    MemberRole role; // 成员角色

    Member(long id, MemberRole role) {
        this.id = id;
        this.role = role;
    }
}

