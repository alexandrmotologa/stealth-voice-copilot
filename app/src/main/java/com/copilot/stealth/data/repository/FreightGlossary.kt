package com.copilot.stealth.data.repository

/**
 * Knowledge entry representing an interview scenario with English and Romanian responses.
 */
data class FreightScenario(
    val category: String,
    val triggerKeywords: List<String>,
    val sampleQuestions: List<String>,
    val answerEn: String,
    val answerRo: String,
    val questionRo: String
)

object FreightGlossary {

    val scenarios: List<FreightScenario> = listOf(
        FreightScenario(
            category = "Detention Policy",
            triggerKeywords = listOf("detention", "waiting", "delayed", "delay", "shipper wait", "receiver wait", "two hours", "free time"),
            sampleQuestions = listOf(
                "What is your standard policy when a driver is delayed at a facility?",
                "How do you handle detention at the shipper or receiver?",
                "What do you do if your driver is waiting past two hours?"
            ),
            answerEn = "Standard industry policy allows two free hours for loading or unloading. I contact the broker at the 90-minute mark to notify them detention is starting, make sure the driver gets their BOL stamped with exact in and out times, and submit our GPS arrival logs for hourly compensation.",
            answerRo = "Standardul este de 2 ore libere. Notifici brokerul la 1.5 ore, ceri stampila cu ora exacta pe BOL si atasezi log-urile GPS pentru plata detentiei ($50-$75/ora).",
            questionRo = "Care este politica ta standard cand un sofer este intarziat la incarcare sau descarcare?"
        ),
        FreightScenario(
            category = "TONU (Truck Ordered Not Used)",
            triggerKeywords = listOf("tonu", "truck ordered not used", "cancelled", "cancel", "load cancelled", "cancelled load"),
            sampleQuestions = listOf(
                "How do you handle a load cancellation after dispatch?",
                "What is your process for claiming a TONU fee?",
                "The broker cancels the load while the driver is already on site. What do you say?"
            ),
            answerEn = "If a load cancels after the driver is dispatched or has arrived at the facility, I immediately request a revised rate confirmation with the standard TONU fee of $150 to $250. Once secured, I verify the driver's location logs and book a replacement load to minimize deadhead.",
            answerRo = "Daca incarcatura se anuleaza dupa ce soferul a plecat sau a ajuns, ceri imediat Rate Con actualizat cu taxa TONU ($150-$250) si cauti o noua cursa pentru a reduce mersul in gol.",
            questionRo = "Cum gestionezi o anulare de cursa dupa ce soferul a fost deja trimis la locatie?"
        ),
        FreightScenario(
            category = "Rate Confirmation Review",
            triggerKeywords = listOf("rate con", "rate confirmation", "contract", "booking", "booking load", "verification", "check rate con"),
            sampleQuestions = listOf(
                "What do you check on a rate confirmation before signing?",
                "Walk me through your checklist when reviewing a rate con.",
                "How do you verify a booking contract with a freight broker?"
            ),
            answerEn = "Before signing, I verify the agreed gross rate, deadhead and loaded mileage, pickup and delivery appointment windows, weight, commodity, and equipment specifications. I also confirm accessorial terms including detention rates, TONU provisions, and lumper reimbursement procedures.",
            answerRo = "Verific pretul total, kilometrii, orele de programare la incarcare/descarcare, greutatea, marfa si clauzele pentru detentie, TONU si decontarea lumper-ului inainte de semnare.",
            questionRo = "Ce verifici pe o confirmare de cursa (Rate Con) inainte de semnare?"
        ),
        FreightScenario(
            category = "Layover Procedure",
            triggerKeywords = listOf("layover", "overnight", "cannot unload", "closed facility", "next day", "rescheduled"),
            sampleQuestions = listOf(
                "What happens if a receiver cannot unload until the next morning?",
                "How do you negotiate layover with a freight broker?",
                "The warehouse is closed when the driver arrives within the window. What do you do?"
            ),
            answerEn = "If a facility pushes unloading to the next calendar day through no fault of our driver, I negotiate a standard layover rate of $200 to $300 per day plus lodging expenses if needed. I ensure the broker confirms this on an updated rate con before authorizing the overnight wait.",
            answerRo = "Daca depozitul amana descarcarea pentru a doua zi, negociezi o taxa de layover de $200-$300 pe zi si confirmi totul in scris pe un Rate Con revizuit.",
            questionRo = "Ce faci daca un depozit nu poate descarca soferul pana a doua zi?"
        ),
        FreightScenario(
            category = "Factoring and Billing Packet",
            triggerKeywords = listOf("factoring", "noa", "notice of assignment", "invoice", "billing", "pod", "proof of delivery", "quick pay"),
            sampleQuestions = listOf(
                "How do you submit invoices for same day factoring funding?",
                "What documents must be included in a complete billing packet?",
                "What is a Notice of Assignment (NOA)?"
            ),
            answerEn = "A complete billing packet requires the signed rate confirmation, clean proof of delivery (POD) with legible receiver signatures and timestamps, and receipts for approved accessorials such as lumpers or scale tickets. We submit this packet through our factoring portal under our Notice of Assignment for same-day funding.",
            answerRo = "Pachetul de facturare contine Rate Con-ul semnat, POD-ul stampilat si semnat de receptor, plus chitantele de lumper/cantar. Se trimite prin factoring pentru plata rapida.",
            questionRo = "Ce documente trebuie sa contina un pachet complet de facturare pentru factoring?"
        ),
        FreightScenario(
            category = "Lumper Reimbursement",
            triggerKeywords = listOf("lumper", "unloading fee", "pallet breakdown", "comcheck", "efs", "receipt"),
            sampleQuestions = listOf(
                "How do you handle lumper fees at grocery warehouses?",
                "What do you do if a driver needs $300 cash for unloading?",
                "How do you get reimbursed for lumper expenses?"
            ),
            answerEn = "When a warehouse requires a third-party lumper, I contact the broker immediately to obtain a Comcheck or EFS advance code before unloading begins. The driver pays the lumper, collects a printed official receipt, and we attach it directly to the delivery paperwork for broker reimbursement.",
            answerRo = "Contactezi brokerul imediat pentru a genera un cod Comcheck sau EFS inainte de descarcare. Soferul plateste, ia bonul fiscal oficial si il atasam la factura.",
            questionRo = "Cum procedezi cand depozitul solicita taxa de descarcare (lumper fee)?"
        ),
        FreightScenario(
            category = "Bill of Lading Discrepancies (OS&D)",
            triggerKeywords = listOf("bol", "bill of lading", "discrepancy", "damaged", "shortage", "damaged cargo", "os&d", "missing pallets"),
            sampleQuestions = listOf(
                "What should the driver do if the cargo count does not match the BOL?",
                "How do you handle damaged goods observed at the receiver?",
                "What is the procedure for an OS&D claim?"
            ),
            answerEn = "The driver must never sign a clean BOL when a discrepancy exists. They must immediately notify dispatch, recount the freight alongside the dock clerk, clearly annotate the missing or damaged items on the BOL, take high-resolution photographs, and notify the broker before departing the dock.",
            answerRo = "Soferul nu semneaza niciodata BOL-ul curat daca marfa lipseste sau e deteriorata. Noteaza exact problema pe document, face poze clare si anunta brokerul inainte de plecare.",
            questionRo = "Ce trebuie sa faca soferul daca numarul de paleti nu corespunde cu scrisoarea de trasura (BOL)?"
        ),
        FreightScenario(
            category = "Hours of Service (HOS) & ELD",
            triggerKeywords = listOf("hos", "hours of service", "eld", "motive", "keeptruckin", "samsara", "11 hours", "14 hours", "34 hour reset"),
            sampleQuestions = listOf(
                "Explain property-carrying Hours of Service regulations to me.",
                "How do you plan a 1000-mile transit under FMCSA HOS rules?",
                "How many hours can a commercial driver operate continuously?"
            ),
            answerEn = "Under FMCSA regulations, drivers have an 11-hour driving window within a 14-hour on-duty period following 10 consecutive hours off duty. They must take a 30-minute break after 8 cumulative hours of driving, and cannot exceed 70 hours in 8 days without a 34-hour restart. I verify remaining drive clocks on the ELD before committing to any appointment.",
            answerRo = "Soferul poate conduce maxim 11 ore intr-o fereastra de 14 ore de munca, dupa 10 ore de pauza obligatorie. O pauza de 30 min e necesara dupa 8 ore. Verifici mereu orele ramase pe ELD.",
            questionRo = "Explica regulile de program de conducere (Hours of Service) conform legislatiei FMCSA."
        ),
        FreightScenario(
            category = "Axle Weight & Cat Scales",
            triggerKeywords = listOf("weight", "scale", "overweight", "cat scale", "axle", "tandem", "80000", "slide tandem", "drive axle"),
            sampleQuestions = listOf(
                "Your driver scales the truck and the drive axle is 35,500 pounds. How do you fix it?",
                "What are legal highway axle weight limits in the United States?",
                "What do you do if total gross weight is 81,200 pounds?"
            ),
            answerEn = "US federal limits are 12,000 pounds on steer axles, 34,000 pounds on drive tandems, and 34,000 pounds on trailer tandems, with an 80,000-pound gross limit. If an axle is over but gross is legal, the driver slides the trailer tandems or fifth wheel to redistribute weight. If gross exceeds 80,000 pounds, the driver must return to the shipper for cargo rework.",
            answerRo = "Limitele legale sunt: 12k lbs pe directie, 34k lbs pe axa tractoare, 34k lbs pe axa semiremorcii, maxim 80k lbs total. Se gliseaza tandemul pentru rebalansare sau se returneaza marfa daca depaseste 80k lbs.",
            questionRo = "Ce faci daca la cantarirea pe CAT Scale o axa depaseste greutatea legala?"
        ),
        FreightScenario(
            category = "Load Board Negotiation",
            triggerKeywords = listOf("dat", "truckstop", "negotiate", "negotiating", "rate negotiation", "rpm", "rate per mile", "lane average"),
            sampleQuestions = listOf(
                "How do you negotiate with freight brokers on DAT One?",
                "What is your strategy to secure rates above the lane average?",
                "Walk me through your process when calling on a posted load."
            ),
            answerEn = "I check the 7-day and 15-day lane averages on DAT One alongside current inbound and outbound load-to-truck ratios. When calling, I emphasize our flawless carrier safety score, verified tracking capabilities, and prompt on-time record to negotiate 10 to 15 percent above spot market averages.",
            answerRo = "Verific mediile de pret pe 7 si 15 zile din DAT One si raportul de marfa/camioane. Subliniez scorul de siguranta al companiei si monitorizarea GPS pentru a obtine un pret peste media pietei.",
            questionRo = "Cum negociezi cu brokerii de marfa pe platforma DAT One?"
        ),
        FreightScenario(
            category = "Breakdowns & Mechanical Delays",
            triggerKeywords = listOf("breakdown", "flat tire", "blowout", "mechanical", "tow", "roadside", "late appointment"),
            sampleQuestions = listOf(
                "Your truck suffers a tire blowout 150 miles from the delivery. What do you do?",
                "How do you manage a mechanical breakdown during a live load?",
                "What is your communication protocol for a delayed load?"
            ),
            answerEn = "First, I ensure the driver is safely pulled onto the shoulder and dispatch mobile roadside assistance immediately. Next, I notify the freight broker with the driver's exact coordinates, diagnosis, and an updated ETA. If the delivery appointment will be missed, I request an appointment reschedule immediately.",
            answerRo = "Te asiguri ca soferul e in siguranta pe banda de urgenta si chemi asistenta mobila (roadside). Anunti imediat brokerul cu locatia GPS, problema si noul ETA, cerand reprogramarea orei.",
            questionRo = "Cum procedezi daca un camion are o defectiune mecanica sau o pana pe traseu?"
        ),
        FreightScenario(
            category = "Reefer Temperature Management",
            triggerKeywords = listOf("reefer", "refrigerated", "temperature", "frozen", "pre-cool", "pulp", "continuous"),
            sampleQuestions = listOf(
                "How do you verify refrigerated freight temperature requirements?",
                "What is the difference between continuous and start-stop reefer modes?",
                "What should the driver check before loading fresh produce?"
            ),
            answerEn = "I confirm the exact temperature setpoint on the rate con and instruct the driver to pre-cool the trailer before arrival. For sensitive produce or meat, the reefer must run on continuous mode, not start-stop. The driver must pulp the product with a calibrated probe thermometer and write the pulp temperatures directly on the BOL before accepting the load.",
            answerRo = "Verifici temperatura pe Rate Con si pre-racesti remorca. Pentru produse sensibile (fructe, carne) se foloseste modul continuu. Soferul masoara temperatura interna a marfei (pulping) si o noteaza pe BOL.",
            questionRo = "Cum asiguri gestionarea corecta a temperaturii la transporturile frigorifice (reefer)?"
        )
    )
}
