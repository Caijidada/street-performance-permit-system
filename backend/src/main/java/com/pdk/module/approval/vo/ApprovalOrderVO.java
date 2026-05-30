package com.pdk.module.approval.vo;

import com.pdk.module.report.entity.ReportOrder;
import lombok.Data;

@Data
public class ApprovalOrderVO {
    private ReportOrder order;
    private String artistName;
    private String artistPhone;
    private Integer creditScore;
    private String venueName;
    private String venueAddress;
    private String venueDistrict;
}
