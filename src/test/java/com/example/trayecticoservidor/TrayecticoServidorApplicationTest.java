package com.example.trayecticoservidor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(MockitoExtension.class)
@DisplayName("Prueba de clase de testing")
class TrayecticoServidorApplicationTest {

    //Metodo de prueba
    int sumar(int a , int b){
        return a+b;
    }




        @Test
        @DisplayName("Suma")
        void testSuma() {
        //given
        int a = 2;
        int b = 2;

        //when
        int resultado = sumar(a,b);

        //then
        assertEquals(4, resultado,"El resultado de 2+2 deberia ser 4");
    }

    @Test
    void main() {

    }

}
