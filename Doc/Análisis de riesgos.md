# 2. Análisis de riesgos

A partir de la discusión con el grupo y el análisis brindado por herramientas de inteligencia artificial, identificamos los siguientes riesgos como los mas significantes para el desarrollo del proyecto. El proceso consistió en revisar en conjunto los riesgos que identificó cada uno, contrastando esa visión con un análisis externo que nos permitió detectar puntos ciegos que no habíamos considerado con suficiente profundidad.
A continuación se listan los riesgos ordenados de mayor a menor en base al impacto que generan sobre el sistema y el cronograma:

| Tipo de Riesgo | Descripción                                                                                                                                                                    | Probabilidad | Impacto |
| -------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------ |:------- |
| Tecnico        | La migración de la Base de Datos al nuevo motor puede ocasionar incompatibilidades.                                                                                            | Alta         | Critico |
| Planificacion  | No cumplir con la fecha límite establecida para una actividad/tarea crítica y que eso retrase al resto de actividades.                                                         | Alta         | Alto    |
| Tecnico        | Expansion Descontrolada del Alcance (Scope Creep): aumento sustancial de nuevas funcionalidades causa retrasos y saturación                                                    | Media        | Alto    |
| Tecnico        | El pobre manejo de excepciones compromete la funcionalidad requerida de capturar fallos y redireccionar usuarios                                                               | Media        | Alto    |
| Tecnico        | Implementación incorrecta / dificultad en implementar la funcionalidad de correlatividades. El riesgo surge de que es la funcionalidad con la lógica mas compleja del proyecto | Media        | Alto    |
| Organizacion   | Baja de un miembro del grupo                                                                                                                                                   | Baja         | Alto    |
| Humano         | El código puede no estar documentado en su totalidad, lo que dificulta la lectura entre los integrantes del equipo.                                                            | Alta         | Medio   |
| Humano         | Curva de aprendizaje respecto a nuevas herramientas y/o lenguajes                                                                                                              | Media        | Medio   |
| Tecnico        | El codigo generado por LLMs es de mala calidad                                                                                                                                 | Baja         | Medio   |
| Tecnico        | Las funcionalidades implementadas pueden ser incorrectas respecto a los requerimientos especificados                                                                           | Baja         | Bajo    |
| Organizacion   | No se definen roles claros a los miembros del equipo                                                                                                                           | Alta         | Bajo    |


