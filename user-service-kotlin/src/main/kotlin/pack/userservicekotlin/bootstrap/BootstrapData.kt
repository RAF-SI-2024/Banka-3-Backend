package pack.userservicekotlin.bootstrap

import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import pack.userservicekotlin.domain.entities.*
import pack.userservicekotlin.repository.*
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.time.LocalDate

@Component
class BootstrapData(
    private val clientRepository: ClientRepository,
    private val employeeRepository: EmployeeRepository,
    private val permissionRepository: PermissionRepository,
    private val activityCodeRepository: ActivityCodeRepository,
    private val passwordEncoder: PasswordEncoder,
    private val companyRepository: CompanyRepository,
    private val roleRepository: RoleRepository,
    private val authTokenRepository: AuthTokenRepository,
    private val actuaryLimitRepository: ActuaryLimitRepository,
    private val authorizedPersonnelRepository: AuthorizedPersonnelRepository,
) : CommandLineRunner {
    override fun run(vararg args: String?) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd")

        if (roleRepository.count() == 0L) {
            val roles =
                listOf("CLIENT", "EMPLOYEE", "ADMIN", "AGENT", "SUPERVISOR").map {
                    Role(name = it)
                }
            roleRepository.saveAll(roles)
        }

        if (clientRepository.count() == 0L) {
            val clientRole = roleRepository.findByName("CLIENT").orElseThrow { RuntimeException("Role CLIENT not found") }

            val clients =
                listOf(
                    Client().apply {
                        firstName = "Marko"
                        lastName = "Markovic"
                        email = "marko.m@example.com"
                        phone = "0611158275"
                        address = "Pozeska 56"
                        birthDate = dateFormat.parse("1990-05-15")
                        gender = "M"
                        password = passwordEncoder.encode("markomarko")
                        jmbg = "0123456789126"
                        username = "marko1"
                        role = clientRole
                    },
                    Client().apply {
                        firstName = "Jovan"
                        lastName = "Jovanovic"
                        email = "jovan.v@example.com"
                        phone = "0671152371"
                        address = "Cara Dusana 105"
                        birthDate = dateFormat.parse("1990-01-25")
                        gender = "M"
                        password = passwordEncoder.encode("jovanjovan")
                        jmbg = "0123456789125"
                        username = "jovan1"
                        role = clientRole
                    },
                )
            clientRepository.saveAll(clients.toSet())
        }

        if (employeeRepository.count() == 0L) {
            val adminRole = roleRepository.findByName("ADMIN").orElseThrow()
            val employeeRole = roleRepository.findByName("EMPLOYEE").orElseThrow()
            val agentRole = roleRepository.findByName("AGENT").orElseThrow()

            val employees =
                listOf(
                    Employee().apply {
                        firstName = "Petar"
                        lastName = "Petrovic"
                        email = "petar.p@example.com"
                        phone = "0699998279"
                        address = "Trg Republike 5, Beograd"
                        birthDate = dateFormat.parse("2000-02-19")
                        gender = "M"
                        username = "petar90"
                        password = passwordEncoder.encode("petarpetar")
                        position = "Manager"
                        department = "HR"
                        active = true
                        jmbg = "0123456789123"
                        role = adminRole
                    },
                    Employee().apply {
                        firstName = "Jana"
                        lastName = "Ivanovic"
                        email = "jana.i@example.com"
                        phone = "0666658276"
                        address = "Palih Boraca 5"
                        birthDate = dateFormat.parse("1996-09-02")
                        gender = "F"
                        username = "jana1"
                        password = passwordEncoder.encode("janajana")
                        position = "Manager"
                        department = "Finance"
                        active = true
                        jmbg = "0123456789124"
                        role = employeeRole
                    },
                    Employee().apply {
                        firstName = "Zika"
                        lastName = "Petrović"
                        email = "zika.p@example.com"
                        phone = "0641234567"
                        address = "Kralja Petra 10"
                        birthDate = dateFormat.parse("1992-05-15")
                        gender = "M"
                        username = "zika92"
                        password = passwordEncoder.encode("zikazika")
                        department = "IT"
                        active = true
                        jmbg = "1505923891234"
                        role = agentRole
                    },
                )
            employeeRepository.saveAll(employees.toSet())
        }

        if (activityCodeRepository.count() == 0L) {
            val codes =
                listOf(
                    "10.01" to "Food production",
                    "62.01" to "Programming",
                    "5.1" to "Coal extraction",
                    "62.09" to "IT",
                    "56.1" to "Restaurants",
                    "86.1" to "Hospital activities",
                    "90.02" to "Museums",
                    "1.11" to "Grain and legume farming",
                    "1.13" to "Vegetable farming",
                    "13.1" to "Textile fiber preparation and spinning",
                    "24.1" to "Iron and steel production",
                    "24.2" to "Steel pipes, hollow profiles, and fittings production",
                    "41.1" to "Construction project development",
                    "41.2" to "Residential and non-residential building construction",
                    "42.11" to "Road and highway construction",
                    "42.12" to "Railway and underground construction",
                    "42.13" to "Bridge and tunnel construction",
                    "42.21" to "Water supply construction projects",
                    "42.22" to "Electric power and telecommunications network construction",
                    "7.1" to "Iron ore extraction",
                    "7.21" to "Uranium and thorium extraction",
                    "8.11" to "Decorative and building stone extraction",
                    "8.92" to "Peat extraction",
                    "47.11" to "Retail trade in non-specialized food and beverage stores",
                    "53.1" to "Postal activities",
                    "53.2" to "Courier activities",
                    "85.1" to "Preschool education",
                    "85.2" to "Primary education",
                    "86.21" to "General medical practice",
                    "86.22" to "Specialist medical practice",
                    "86.9" to "Other healthcare activities",
                    "84.12" to "Regulation of economic activities",
                    "90.01" to "Theater activities",
                    "90.04" to "Botanical and zoological gardens",
                    "93.11" to "Operation of sports facilities",
                    "93.13" to "Gym activities",
                    "93.19" to "Other sports activities",
                    "26.11" to "Electronic components manufacturing",
                    "27.12" to "Electrical panels and boards manufacturing",
                    "29.1" to "Motor vehicle manufacturing",
                ).map { (id, desc) -> ActivityCode(id = id, description = desc) }
            activityCodeRepository.saveAll(codes)
        }

        if (companyRepository.count() == 0L) {
            val owner = clientRepository.findById(1L).orElse(null)
            val companies =
                listOf(
                    Company(
                        name = "Banka 3",
                        address = "Adresa banke",
                        activityCode = "10.02",
                        registrationNumber = "11111111",
                        taxId = "111111111111111",
                    ),
                    Company(
                        name = "Republika Srbija",
                        address = "Adresa drzave",
                        activityCode = "10.02",
                        registrationNumber = "1",
                        taxId = "1",
                    ),
                    Company(
                        name = "Microsoft",
                        address = "Adresa microsofta",
                        activityCode = "10.01",
                        registrationNumber = "3351361632441",
                        taxId = "36232343",
                        majorityOwner = owner,
                    ),
                )
            companyRepository.saveAll(companies)
        }

        if (authTokenRepository.count() == 0L) {
            val tokens =
                listOf(
                    AuthToken(
                        token = "df7ff5f0-70bd-492c-9569-ac5f3fbda7ff",
                        type = "set-password",
                        createdAt = 1741631004271L,
                        expiresAt = 3000000000000L,
                        userId = 6L,
                    ),
                    AuthToken(
                        token = "df7ff5f0-70bd-492c-9569-ac5f3fbda7xd",
                        type = "request-card",
                        createdAt = 1741631004271L,
                        expiresAt = 3000000000000L,
                        userId = 6L,
                    ),
                )
            authTokenRepository.saveAll(tokens)
        }

        if (actuaryLimitRepository.count() == 0L) {
            val e2 = employeeRepository.findByEmail("zika.p@example.com").orElseThrow()
            val al =
                ActuaryLimit(limitAmount = BigDecimal("10000.00"), usedLimit = BigDecimal("2500.00"), needsApproval = false, employee = e2)
            actuaryLimitRepository.save(al)
        }

        if (authorizedPersonnelRepository.count() == 0L) {
            val company1 = companyRepository.findById(3L).orElseThrow()
            val auth1 =
                AuthorizedPersonel(
                    address = "Adresa Janka",
                    company = company1,
                    dateOfBirth = LocalDate.of(2000, 1, 1),
                    gender = "M",
                    firstName = "Janko",
                    lastName = "Stefanovic",
                    email = "email@email.com",
                    phoneNumber = "0691153492",
                )
            authorizedPersonnelRepository.save(auth1)
        }
    }
}
