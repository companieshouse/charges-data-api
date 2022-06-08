package uk.gov.companieshouse.charges.data.converter;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.api.charges.ClassificationApi.TypeEnum.CHARGE_DESCRIPTION;

@ExtendWith(MockitoExtension.class)
public class EnumConvertersTest {

    @Mock
    private TypeDescriptor typeDescriptor;

    private EnumConverters.StringToEnum stringToEnum;
    private EnumConverters.EnumToString enumToString;

    @BeforeEach
    void setUp() {
        stringToEnum = new EnumConverters.StringToEnum();
        enumToString = new EnumConverters.EnumToString();
    }

    @Test
    void testGetConvertibleTypes() {
        assertEquals(Set.of(new GenericConverter.ConvertiblePair(String.class, Enum.class)), stringToEnum.getConvertibleTypes());
        assertEquals(Set.of(new GenericConverter.ConvertiblePair(Enum.class, String.class)), enumToString.getConvertibleTypes());
    }

    @Test
    void testConvertException() {
        when(typeDescriptor.getType()).thenThrow(new IllegalArgumentException());

        assertThrows(IllegalArgumentException.class, () -> stringToEnum.convert(null, typeDescriptor, typeDescriptor));
        assertEquals("CHARGE_DESCRIPTION", enumToString.convert(CHARGE_DESCRIPTION, typeDescriptor,
                typeDescriptor));
    }
}