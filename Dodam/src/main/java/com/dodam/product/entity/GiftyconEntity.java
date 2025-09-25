package com.dodam.product.entity;

//import com.dodam.member.entity.MemberEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "giftycon")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GiftyconEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "giftyconnum")
    private Long giftyconnum;

    @Column(name = "giftyconname", nullable = false, length = 200)
    private String giftyconname;

    @Lob
    @Column(name = "giftyimg", nullable = false)
    private byte[] giftyimg;

    @Column(name = "giftstart")
    private LocalDate giftstart;

    @Column(name = "giftend")
    private LocalDate giftend;

//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(name = "rewardnum", nullable = false)
//    private EventRewardEntity eventReward;

//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(name = "mnum", nullable = false)
//    private MemberEntity member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "giftcate", nullable = false)
    private GifyconTypeEntity giftcate;
}
