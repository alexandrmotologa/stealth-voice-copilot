package com.copilot.stealth.data.models

import kotlinx.serialization.Serializable

@Serializable
data class CarrierProfile(
    val companyName: String = "Apex Freight Logistics LLC",
    val mcNumber: String = "MC-1094821",
    val dotNumber: String = "USDOT 3481920",
    val fleetSize: Int = 18,
    val equipmentTypes: String = "53ft Air-Ride Dry Vans & Thermo King Reefer Units",
    val factoringCompany: String = "Triumph Business Capital (Notice of Assignment active for same-day ACH)",
    val standardDetentionRate: Double = 50.0,
    val standardTonuFee: Double = 200.0,
    val homeBase: String = "Chicago, IL"
)
