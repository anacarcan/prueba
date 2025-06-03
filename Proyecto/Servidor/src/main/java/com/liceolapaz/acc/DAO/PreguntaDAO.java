package com.liceolapaz.acc.DAO;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liceolapaz.acc.entidades.Pregunta;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class PreguntaDAO {

    // Registro de servicios estándar de Hibernate configurado desde hibernate.cfg.xml
    private static final StandardServiceRegistry sr =
            new StandardServiceRegistryBuilder().configure().build();

    // Fábrica de sesiones de Hibernate para gestionar conexiones a la base de datos
    private static final SessionFactory sf =
            new MetadataSources(sr).buildMetadata().buildSessionFactory();

    /**
     * Obtiene preguntas aleatorias de una categoría específica
     * Si no existen preguntas en la base de datos, las carga automáticamente desde JSON
     * @param categoria Categoría de las preguntas a obtener
     * @param cantidad Número de preguntas a devolver
     * @return Lista de preguntas aleatorias de la categoría especificada
     */
    public static List<Pregunta> obtenerPreguntasPorCategoria(String categoria, int cantidad) {
        Session session = sf.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            System.out.println("🔍 Buscando preguntas para categoría: " + categoria);

            // Verificar si existen preguntas para esta categoría
            long count = (long) session.createQuery(
                            "SELECT COUNT(p) FROM Pregunta p WHERE p.categoria = :categoria AND p.activa = true")
                    .setParameter("categoria", categoria.toLowerCase())
                    .uniqueResult();

            System.out.println("📊 Preguntas encontradas en BD: " + count);

            // Si no hay preguntas, cargar desde JSON
            if (count == 0) {
                System.out.println("📥 Cargando preguntas desde JSON para: " + categoria);
                cargarPreguntasDesdeJSON(categoria.toLowerCase(), session);
                transaction.commit();

                // Reiniciar transacción para la consulta
                transaction = session.beginTransaction();
                count = (long) session.createQuery(
                                "SELECT COUNT(p) FROM Pregunta p WHERE p.categoria = :categoria AND p.activa = true")
                        .setParameter("categoria", categoria.toLowerCase())
                        .uniqueResult();
                System.out.println("✅ Preguntas cargadas: " + count);
            }

            // Obtener preguntas aleatorias usando ORDER BY RAND()
            List<Pregunta> preguntas = session.createQuery(
                            "FROM Pregunta WHERE categoria = :categoria AND activa = true ORDER BY RAND()",
                            Pregunta.class)
                    .setParameter("categoria", categoria.toLowerCase())
                    .setMaxResults(cantidad)
                    .getResultList();

            transaction.commit();

            System.out.println("🎯 Devolviendo " + preguntas.size() + " preguntas para " + categoria);

            // Debug de preguntas cargadas para verificar integridad
            for (int i = 0; i < preguntas.size(); i++) {
                Pregunta p = preguntas.get(i);
                System.out.println("   DEBUG Pregunta " + (i+1) + " (ID:" + p.getId() + "): " +
                        "Respuesta correcta = " + p.getRespuestaCorrecta() + " (" + p.getLetraRespuesta() + ")");
            }

            return preguntas;

        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            System.out.println("❌ Error obteniendo preguntas: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            session.close();
        }
    }

    /**
     * Carga preguntas desde un archivo JSON con validación mejorada
     * Convierte índices de respuestas de JSON (1-4) a base de datos (0-3)
     * @param categoria Categoría de las preguntas a cargar
     * @param session Sesión de Hibernate activa para persistir las preguntas
     */
    private static void cargarPreguntasDesdeJSON(String categoria, Session session) {
        try {
            String nombreArchivo = "preguntas-" + categoria + ".json";
            System.out.println("📂 Intentando cargar: " + nombreArchivo);

            // Buscar archivo JSON en classpath
            InputStream inputStream = PreguntaDAO.class.getClassLoader().getResourceAsStream(nombreArchivo);

            if (inputStream == null) {
                System.out.println("❌ Archivo no encontrado: " + nombreArchivo);
                return;
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(inputStream);

            int preguntasCargadas = 0;
            for (JsonNode node : rootNode) {
                try {
                    Pregunta pregunta = new Pregunta();

                    // Extraer datos del JSON
                    pregunta.setTextoPregunta(node.get("pregunta").asText());

                    // Cargar las cuatro opciones de respuesta
                    JsonNode opciones = node.get("opciones");
                    pregunta.setOpcionA(opciones.get(0).asText());
                    pregunta.setOpcionB(opciones.get(1).asText());
                    pregunta.setOpcionC(opciones.get(2).asText());
                    pregunta.setOpcionD(opciones.get(3).asText());

                    // CORREGIDO: Conversión correcta de índices JSON a base de datos
                    int respuestaJSON = node.get("respuestaCorrecta").asInt();
                    // JSON usa índices 1-4, convertir a 0-3 para la base de datos
                    int respuestaDB = respuestaJSON - 1;

                    // VALIDACIÓN: Asegurar que el índice esté en rango válido
                    if (respuestaDB < 0 || respuestaDB > 3) {
                        System.out.println("❌ ERROR: Respuesta correcta fuera de rango en JSON: " + respuestaJSON +
                                " para pregunta: " + pregunta.getTextoPregunta());
                        continue; // Saltar esta pregunta
                    }

                    pregunta.setRespuestaCorrecta(respuestaDB);

                    // Configurar metadatos de la pregunta
                    pregunta.setCategoria(categoria);
                    pregunta.setDificultad(node.has("dificultad") ? node.get("dificultad").asText() : "medio");
                    pregunta.setActiva(true);

                    // Guardar en la base de datos
                    session.persist(pregunta);
                    preguntasCargadas++;

                    // CORREGIDO: Log mejorado para depuración
                    System.out.println("📝 Pregunta " + preguntasCargadas + " cargada:");
                    System.out.println("   Texto: " + pregunta.getTextoPregunta().substring(0, Math.min(50, pregunta.getTextoPregunta().length())) + "...");
                    System.out.println("   Respuesta JSON: " + respuestaJSON + " -> DB: " + respuestaDB + " (" + pregunta.getLetraRespuesta() + ")");
                    System.out.println("   Respuesta texto: " + pregunta.getRespuestaTexto());

                } catch (Exception e) {
                    System.out.println("❌ Error procesando pregunta individual: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            System.out.println("✅ Total cargadas para " + categoria + ": " + preguntasCargadas + " preguntas");

        } catch (Exception e) {
            System.out.println("❌ Error cargando JSON: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al cargar preguntas desde JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene todas las categorías disponibles en el sistema
     * Si no hay categorías en la base de datos, devuelve categorías predeterminadas
     * @return Lista de categorías disponibles
     */
    public static List<String> obtenerCategorias() {
        Session session = sf.openSession();
        try {
            // Intentar obtener categorías de la base de datos
            List<String> categorias = session.createQuery(
                            "SELECT DISTINCT p.categoria FROM Pregunta p WHERE p.activa = true",
                            String.class)
                    .getResultList();

            // Si no hay categorías en BD, usar las predeterminadas
            if (categorias.isEmpty()) {
                categorias = List.of("conocimiento-general", "musica", "geografia", "deportes");
                System.out.println("📚 Usando categorías predeterminadas: " + categorias);
            } else {
                System.out.println("📚 Categorías en BD: " + categorias);
            }

            return categorias;

        } catch (Exception e) {
            System.out.println("❌ Error obteniendo categorías: " + e.getMessage());
            // Devolver categorías por defecto en caso de error
            return List.of("conocimiento-general", "musica", "geografia", "deportes");
        } finally {
            session.close();
        }
    }

    /**
     * Cuenta el número de preguntas activas por categoría
     * @param categoria Categoría a consultar
     * @return Número de preguntas en la categoría
     */
    public static long contarPreguntasPorCategoria(String categoria) {
        Session session = sf.openSession();
        try {
            // Consulta HQL para contar preguntas activas por categoría
            Long count = (Long) session.createQuery(
                            "SELECT COUNT(p) FROM Pregunta p WHERE p.categoria = :categoria AND p.activa = true")
                    .setParameter("categoria", categoria.toLowerCase())
                    .uniqueResult();

            long resultado = count != null ? count : 0;
            System.out.println("📊 Preguntas en " + categoria + ": " + resultado);
            return resultado;

        } catch (Exception e) {
            System.out.println("❌ Error contando preguntas: " + e.getMessage());
            return 0;
        } finally {
            session.close();
        }
    }

    /**
     * Obtiene una pregunta específica por su ID
     * @param id ID de la pregunta a buscar
     * @return La pregunta encontrada o null si no existe
     */
    public static Pregunta obtenerPreguntaPorId(int id) {
        Session session = sf.openSession();
        try {
            Pregunta pregunta = session.get(Pregunta.class, id);
            if (pregunta != null) {
                System.out.println("🔍 Pregunta encontrada: " + pregunta.getTextoPregunta());
                System.out.println("   Respuesta correcta: " + pregunta.getRespuestaCorrecta() + " (" + pregunta.getLetraRespuesta() + ")");
            } else {
                System.out.println("❌ Pregunta no encontrada con ID: " + id);
            }
            return pregunta;
        } catch (Exception e) {
            System.out.println("❌ Error obteniendo pregunta por ID: " + e.getMessage());
            return null;
        } finally {
            session.close();
        }
    }

    /**
     * Valida la integridad de las preguntas cargadas en una categoría
     * Verifica que las respuestas correctas estén en rango válido y las opciones no estén vacías
     * @param categoria Categoría a validar
     */
    public static void validarIntegridadPreguntas(String categoria) {
        Session session = sf.openSession();
        try {
            // Obtener todas las preguntas activas de la categoría
            List<Pregunta> preguntas = session.createQuery(
                            "FROM Pregunta WHERE categoria = :categoria AND activa = true",
                            Pregunta.class)
                    .setParameter("categoria", categoria.toLowerCase())
                    .getResultList();

            System.out.println("🔍 Validando " + preguntas.size() + " preguntas de " + categoria);

            for (Pregunta p : preguntas) {
                boolean valida = true;
                String errores = "";

                // Validar rango de respuesta correcta (debe estar entre 0-3)
                if (p.getRespuestaCorrecta() < 0 || p.getRespuestaCorrecta() > 3) {
                    valida = false;
                    errores += "Respuesta fuera de rango (" + p.getRespuestaCorrecta() + "). ";
                }

                // Validar que las opciones no estén vacías
                if (p.getOpcionA() == null || p.getOpcionA().trim().isEmpty() ||
                        p.getOpcionB() == null || p.getOpcionB().trim().isEmpty() ||
                        p.getOpcionC() == null || p.getOpcionC().trim().isEmpty() ||
                        p.getOpcionD() == null || p.getOpcionD().trim().isEmpty()) {
                    valida = false;
                    errores += "Opciones vacías. ";
                }

                if (!valida) {
                    System.out.println("❌ Pregunta ID:" + p.getId() + " INVÁLIDA: " + errores);
                } else {
                    System.out.println("✅ Pregunta ID:" + p.getId() + " válida (Respuesta: " + p.getLetraRespuesta() + ")");
                }
            }

        } catch (Exception e) {
            System.out.println("❌ Error validando preguntas: " + e.getMessage());
        } finally {
            session.close();
        }
    }

    /**
     * Elimina todas las preguntas de la base de datos (útil para testing y limpieza)
     */
    public static void eliminarTodasLasPreguntas() {
        Session session = sf.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            // Ejecutar operación de eliminación masiva
            int eliminadas = session.createQuery("DELETE FROM Pregunta").executeUpdate();
            transaction.commit();
            System.out.println("🗑️ Eliminadas " + eliminadas + " preguntas de la base de datos");
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            System.out.println("❌ Error eliminando preguntas: " + e.getMessage());
        } finally {
            session.close();
        }
    }

    /**
     * Cierra la SessionFactory y libera recursos de Hibernate
     */
    public static void cerrarFactory() {
        try {
            if (sf != null) {
                sf.close();
            }
            if (sr != null) {
                StandardServiceRegistryBuilder.destroy(sr);
            }
            System.out.println("🔒 Factory de Hibernate cerrada");
        } catch (Exception e) {
            System.out.println("❌ Error cerrando factory: " + e.getMessage());
        }
    }
}