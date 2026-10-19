## Patron Singleton

<img width="636" height="381" alt="clases_con_patron_singleton" src="https://github.com/user-attachments/assets/3ba25ca9-ca1a-4716-b673-5a130aa6a7cd" />

**Por qué está.** Antes de tocar una pantalla conviene saber de qué depende. Esta vista lo resuelve pantalla por pantalla, incluidos los recursos que no son código.

**Qué muestra.** Son 20 componentes y 14 interfaces consumidas, con 17 conexiones. Cada componente agrupa los archivos de su pantalla: el .fxml, el controlador y, cuando la tiene, su hoja de estilos. Inventario y el mapa de salón no tienen hoja propia, y Main no tiene .fxml porque arma su interfaz en código. Las hojas de estilo las carga Main y no cada controlador, que es una dependencia fácil de pasar por alto.
