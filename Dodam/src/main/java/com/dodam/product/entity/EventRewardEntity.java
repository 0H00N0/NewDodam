package com.dodam.product.entity;

//import com.dodam.member.entity.MemberEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "eventreward")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventRewardEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rewardnum")
    private Long rewardnum;

    @Column(name = "rewardtype", nullable = false, length = 100)
    private String rewardtype;

    @Column(name = "rewardstatus", length = 50)
    private String rewardstatus;

    @Column(name = "likesnapshot", nullable = false)
    private Long likesnapshot;

    @Column(name = "awarddate", nullable = false)
    private LocalDateTime awarddate;

    @Column(name = "deliverydate")
    private LocalDateTime deliverydate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revnum", nullable = false)
    private ReviewEntity review;

//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(name = "mnum", nullable = false)
//    private MemberEntity member;
}

