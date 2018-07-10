\ LP format - for model browsing. Use MPS format to capture full model detail.
Minimize
  res
Subject To
 R_BS: BS = 0
 R_ST_exo: ST_exo = 1
 R_BT_exo: BT_exo = 1
 R_~SH_|_BS: - SH + BS >= 0
 R_~BH_|_BS: - BH + BS >= 0
 R_~XT_|_SH: - XT + SH >= 0
 R_~BH_|_BT: BT - BH >= 0
 R_~BH_|_~SH: - SH - BH >= -1
 R_~ST_exo_|_XT: - ST_exo + XT >= 0
 R_result: BT + res = 1
Bounds
 res = 1
Binaries
 ST_exo BT_exo XT SH BT BH BS
Generals
 res
End
