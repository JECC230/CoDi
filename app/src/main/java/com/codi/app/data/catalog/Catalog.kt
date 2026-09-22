package com.codi.app.data.catalog

import com.codi.app.data.CoDiProfile
import kotlin.random.Random

/**
 * Catálogo de CoDi v1: los 100 perfiles ficticios del mazo de Home. Es contenido de la app (como las imágenes o
 * los textos), así que viaja dentro del APK; lo que el usuario hace con él
 * -a quién le da "Me Interesa" o "Pasar", sus chats- se guarda en la nube
 * (ver [com.codi.app.data.remote.UserDataStore]).
 */
object Catalog {

    /**
     * Los primeros 5 perfiles están escritos a mano porque las conversaciones
     * de la app anterior los referenciaban por nombre. El resto del mazo (hasta
     * 100 en total) se genera de forma procedural en [generatedProfiles]
     * para tener abundancia de datos variados en pruebas y demo.
     */
    private val handWrittenProfiles = listOf(
        CoDiProfile(
            id = "p1",
            name = "María López",
            age = 24,
            zone = "Zona Norte",
            colorSeed = 0,
            interests = listOf("Cocinar", "Música", "Series", "Yoga"),
            bio = "Diseñadora UX, tranquila y ordenada. Busco CoDi con horarios similares " +
                "para compartir un depa cerca del metro y, de vez en cuando, una buena cena.",
            compatibility = 92,
            occupation = "Diseñadora UX",
            budget = 4000,
            schedule = "Oficina de 9 a 6, casi siempre en casa por la noche",
            cleanliness = 5,
            smoker = false,
            petFriendly = true,
            moveInDate = "1 de octubre",
        ),
        CoDiProfile(
            id = "p2",
            name = "Daniel Torres",
            age = 27,
            zone = "Roma Norte",
            colorSeed = 1,
            interests = listOf("Gym", "Cine", "Cocinar", "Perros"),
            bio = "Ingeniero de software, trabajo híbrido. Me gusta el orden pero no soy " +
                "obsesivo. Tengo un perro pequeño y busco depa pet friendly para compartir.",
            compatibility = 81,
            occupation = "Ingeniero de software",
            budget = 5200,
            schedule = "Híbrido, dos días en oficina y el resto desde casa",
            cleanliness = 4,
            smoker = false,
            petFriendly = true,
            moveInDate = "15 de octubre",
        ),
        CoDiProfile(
            id = "p3",
            name = "Camila Ruiz",
            age = 23,
            zone = "Condesa",
            colorSeed = 2,
            interests = listOf("Arte", "Yoga", "Plantas", "Café"),
            bio = "Estudiante de posgrado, casi no estoy en depa entre semana. Busco un " +
                "espacio tranquilo, con buena luz y CoDis respetuosos del silencio.",
            compatibility = 88,
            occupation = "Estudiante de posgrado",
            budget = 3500,
            schedule = "Clases y laboratorio todo el día, llego tarde entre semana",
            cleanliness = 4,
            smoker = false,
            petFriendly = false,
            moveInDate = "1 de noviembre",
        ),
        CoDiProfile(
            id = "p4",
            name = "Jorge Medina",
            age = 26,
            zone = "Nápoles",
            colorSeed = 3,
            interests = listOf("Videojuegos", "Música", "Cocinar", "Series"),
            bio = "Freelancer creativo, horario flexible. Me encanta cocinar para el " +
                "CoDi en turno y armar noches de juegos de mesa los viernes.",
            compatibility = 75,
            occupation = "Diseñador freelance",
            budget = 4500,
            schedule = "Flexible, trabajo desde casa la mayor parte del tiempo",
            cleanliness = 3,
            smoker = false,
            petFriendly = true,
            moveInDate = "Inmediata",
        ),
        CoDiProfile(
            id = "p5",
            name = "Valeria Sánchez",
            age = 25,
            zone = "Del Valle",
            colorSeed = 4,
            interests = listOf("Running", "Series", "Café", "Yoga"),
            bio = "Contadora, disciplinada con mis horarios y muy limpia. Busco CoDi " +
                "que respete espacios comunes y con quien compartir el café de las mañanas.",
            compatibility = 84,
            occupation = "Contadora",
            budget = 3800,
            schedule = "Entro a las 8 am y salgo a las 5 pm, duermo temprano",
            cleanliness = 5,
            smoker = false,
            petFriendly = false,
            moveInDate = "1 de octubre",
        )
    )

    private val FIRST_NAMES = listOf(
        "Andrea", "Luis", "Fernanda", "Diego", "Paola", "Ricardo", "Ana", "Miguel", "Sofía", "Carlos",
        "Gabriela", "Alejandro", "Renata", "Emilio", "Ximena", "Rodrigo", "Isabel", "Sergio", "Daniela", "Iván",
        "Mariana", "Héctor", "Paulina", "Óscar", "Regina", "Adrián", "Julián", "Carolina", "Mauricio", "Alejandra",
        "Gerardo", "Natalia", "Raúl", "Lucía", "Fabián", "Patricia", "Rubén", "Montserrat", "Eduardo", "Karla",
        "Tomás", "Abigail", "Leonardo", "Yolanda", "Arturo", "Brenda", "Ignacio", "Claudia", "Vicente", "Elena"
    )

    private val LAST_NAMES = listOf(
        "Hernández", "García", "Martínez", "Pérez", "Gómez", "Díaz", "Reyes", "Morales", "Jiménez", "Vargas",
        "Castro", "Romero", "Ortiz", "Chávez", "Ramírez", "Cruz", "Mendoza", "Aguilar", "Herrera", "Guerrero",
        "Rojas", "Navarro", "Cortés", "Delgado", "Flores", "Sánchez", "Torres", "Ruiz", "Medina", "López"
    )

    private val ZONES = listOf(
        "Zona Norte", "Roma Norte", "Condesa", "Nápoles", "Del Valle", "Polanco", "Coyoacán", "Narvarte",
        "Doctores", "Escandón", "Álamos", "Portales", "Anzures", "Juárez", "Tabacalera", "San Rafael",
        "Santa María la Ribera", "Álamos", "Iztaccíhuatl", "Tlalpan Centro"
    )

    private val OCCUPATIONS = listOf(
        "Diseñador/a UX", "Ingeniero/a de software", "Estudiante de posgrado", "Diseñador/a freelance",
        "Contador/a", "Médico/a", "Abogado/a", "Arquitecto/a", "Fotógrafo/a", "Chef", "Profesor/a",
        "Enfermero/a", "Analista de datos", "Community manager", "Psicólogo/a", "Contratista",
        "Escritor/a", "Músico/a", "Ilustrador/a", "Traductor/a"
    )

    private val SCHEDULES = listOf(
        "Oficina de 9 a 6, casi siempre en casa por la noche",
        "Híbrido, dos días en oficina y el resto desde casa",
        "Clases y laboratorio todo el día, llego tarde entre semana",
        "Flexible, trabajo desde casa la mayor parte del tiempo",
        "Entro a las 8 am y salgo a las 5 pm, duermo temprano",
        "Turno vespertino, casi siempre despierto/a hasta tarde",
        "Freelance con horarios variables según proyecto",
        "Guardias rotativas, mi horario cambia semana a semana"
    )

    private val MOVE_IN_DATES = listOf(
        "Inmediata", "1 de octubre", "15 de octubre", "1 de noviembre", "15 de noviembre", "1 de diciembre"
    )

    private val INTEREST_POOL = listOf(
        "Cocinar", "Música", "Series", "Yoga", "Gym", "Cine", "Perros", "Arte", "Plantas", "Café",
        "Videojuegos", "Running", "Fotografía", "Lectura", "Viajes", "Baile", "Senderismo", "Meditación",
        "Música/DJ", "Deportes", "Mascotas"
    )

    /** Perfiles p6..p100, generados con una semilla fija para que el catálogo sea siempre el mismo. */
    private val generatedProfiles: List<CoDiProfile> = run {
        val random = Random(42)
        (6..100).map { index ->
            val first = FIRST_NAMES[random.nextInt(FIRST_NAMES.size)]
            val last = LAST_NAMES[random.nextInt(LAST_NAMES.size)]
            val zone = ZONES[random.nextInt(ZONES.size)]
            val occupation = OCCUPATIONS[random.nextInt(OCCUPATIONS.size)]
            val interests = INTEREST_POOL.shuffled(random).take(3 + random.nextInt(2))
            val cleanliness = 1 + random.nextInt(5)
            val schedule = SCHEDULES[random.nextInt(SCHEDULES.size)]
            val petFriendly = random.nextBoolean()

            CoDiProfile(
                id = "p$index",
                name = "$first $last",
                age = 19 + random.nextInt(18),
                zone = zone,
                colorSeed = index % 6,
                interests = interests,
                bio = "$occupation${if (random.nextBoolean()) ", en $zone" else ""}. " +
                    "${if (cleanliness >= 4) "Ordenado/a y tranquilo/a" else "Relajado/a con el orden"}, " +
                    "busco CoDi con quien compartir gastos y buena onda.",
                compatibility = 55 + random.nextInt(45),
                occupation = occupation,
                budget = 2500 + random.nextInt(15) * 200,
                schedule = schedule,
                cleanliness = cleanliness,
                smoker = random.nextInt(10) < 2,
                petFriendly = petFriendly,
                moveInDate = MOVE_IN_DATES[random.nextInt(MOVE_IN_DATES.size)],
            )
        }
    }

    /** Mazo completo: 100 perfiles (5 de mano + 95 generados). */
    val profiles: List<CoDiProfile> = handWrittenProfiles + generatedProfiles
}
