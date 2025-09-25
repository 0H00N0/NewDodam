package com.dodam.product.entity;

//import com.dodam.member.entity.MemberEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "giftconstate")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GiftconStateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "giftstaenum")
    private Long giftstaenum;

    @Column(name = "giftstate", nullable = false)
    private Integer giftstate; // 0=비전송, 1=전송

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "giftyconnum", nullable = false)
    private GiftyconEntity giftycon;

//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(name = "rewardnum", nullable = false)
//    private EventRewardEntity eventReward;
//
//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(name = "mnum", nullable = false)
//    private MemberEntity member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "giftcate", nullable = false)
    private GifyconTypeEntity giftcate;
}
