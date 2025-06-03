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

    private static final StandardServiceRegistry sr =
            new StandardServiceRegistryBuilder().configure().build();

    private static final SessionFactory sf =
            new MetadataSources(sr).buildMetadata().buildSessionFactory();

    /**
     * FIXED: Obtiene preguntas aleatorias de una categoría específica
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

            // Obtener preguntas aleatorias
            List<Pregunta> preguntas = session.createQuery(
                            "FROM Pregunta WHERE categoria = :categoria AND activa = true ORDER BY RAND()",
                            Pregunta.class)
                    .setParameter("categoria", categoria.toLowerCase())
                    .setMaxResults(cantidad)
                    .getResultList();

            transaction.commit();

            System.out.println("🎯 Devolviendo " + preguntas.size() + " preguntas para " + categoria);

            // FIXED: Debug de preguntas cargadas
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
     * FIXED: Carga preguntas desde un archivo JSON con validación mejorada
     */
    private static void cargarPreguntasDesdeJSON(String categoria, Session session) {
        try {
            String nombreArchivo = "preguntas-" + categoria + ".json";
            System.out.println("📂 Intentando cargar: " + nombreArchivo);

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

                    JsonNode opciones = node.get("opciones");
                    pregunta.setOpcionA(opciones.get(0).asText());
                    pregunta.setOpcionB(opciones.get(1).asText());
                    pregunta.setOpcionC(opciones.get(2).asText());
                    pregunta.setOpcionD(opciones.get(3).asText());

                    // FIXED: Conversión correcta de índices JSON a base de datos
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

                    pregunta.setCategoria(categoria);
                    pregunta.setDificultad(node.has("dificultad") ? node.get("dificultad").asText() : "medio");
                    pregunta.setActiva(true);

                    // Guardar en la base de datos
                    session.persist(pregunta);
                    preguntasCargadas++;

                    // FIXED: Log mejorado para depuración
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
     * Obtiene todas las categorías disponibles
     */
    public static List<String> obtenerCategorias() {
        Session session = sf.openSession();
        try {
            // Intentar obtener de la base de datos
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
     * Cuenta preguntas por categoría
     */
    public static long contarPreguntasPorCategoria(String categoria) {
        Session session = sf.openSession();
        try {
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
     * Obtiene una pregunta por ID
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
     * NUEVO: Método para validar integridad de preguntas cargadas
     */
    public static void validarIntegridadPreguntas(String categoria) {
        Session session = sf.openSession();
        try {
            List<Pregunta> preguntas = session.createQuery(
                            "FROM Pregunta WHERE categoria = :categoria AND activa = true",
                            Pregunta.class)
                    .setParameter("categoria", categoria.toLowerCase())
                    .getResultList();

            System.out.println("🔍 Validando " + preguntas.size() + " preguntas de " + categoria);

            for (Pregunta p : preguntas) {
                boolean valida = true;
                String errores = "";

                // Validar rango de respuesta correcta
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
     * Elimina todas las preguntas (útil para testing)
     */
    public static void eliminarTodasLasPreguntas() {
        Session session = sf.openSession();
        Transaction transaction = session.beginTransaction();

        try {
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
     * Cierra la factory de Hibernate
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