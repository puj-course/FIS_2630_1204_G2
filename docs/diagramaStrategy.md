# Patrón Strategy · HU #294 y #281 · el descuento de la cuenta

<img width="1760" height="1300" alt="imagen" src="https://github.com/user-attachments/assets/6d050ccc-5f85-40d9-a5b2-e1d5949f9039" />


**Por qué está.** El cajero puede descontar por monto fijo o por porcentaje y el campo donde teclea es el mismo, así que el número significa cosas distintas según el tipo: misma entrada, misma salida, algoritmo distinto. Esta vista responde dónde vive cada cálculo y quién elige.

**Qué muestra.** Son 5 cajas y 7 flechas, 2 de ellas de realización, con 2 atributos y 13 operaciones. La interfaz declara las dos operaciones que toda estrategia comparte y debajo cuelgan las dos implementaciones. CalculadoraDescuento es el contexto y guarda una estrategia por agregación. Lo interesante está en hacia dónde van las cuatro flechas del controlador: crea las dos estrategias, le fija una a la calculadora, y después pide calcular sobre DescuentoStrategy, no sobre la calculadora. Por eso CalculadoraDescuento no recibe ninguna flecha de cálculo: el contexto está puenteado y el diagrama lo muestra tal como es.
