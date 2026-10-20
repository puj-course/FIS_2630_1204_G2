# Patrón Factory · las alertas de stock del inventario

<img width="1860" height="1080" alt="imagen" src="https://github.com/user-attachments/assets/f51108bb-06b3-4a5c-b905-cdec5b96d99c" />


**Por qué está.** La pantalla de inventario decidía en tres sitios distintos el texto de la columna de alerta y los dos estilos, comparando cantidades a mano cada vez. Esta vista responde quién decide ahora qué alerta corresponde a un ingrediente.

**Qué muestra.** El controller llama a AlertaStockFactory.para(ingrediente), que elige el creator según el stock:

Stock cero o menos: AlertaAgotadoFactory, que crea AlertaAgotado.
Stock en el mínimo o por debajo: AlertaStockBajoFactory, que crea AlertaStockBajo.
Cualquier otro caso: AlertaNormalFactory, que crea AlertaNormal.
Después llama a crearAlerta() y usa el resultado a través de AlertaStock.
