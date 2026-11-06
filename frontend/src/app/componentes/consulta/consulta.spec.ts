import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PuntoSeleccionable } from '../../api/tipos';
import { Consulta, SolicitudRuta } from './consulta';

const PUNTOS: PuntoSeleccionable[] = [
  { id: 28, nombre: 'Entrada de Estudiantes (Parqueadero)', tipo: 'ENTRADA_CAMPUS', edificio: null },
  { id: 1, nombre: 'Entrada Principal', tipo: 'ENTRADA_CAMPUS', edificio: null },
  { id: 2, nombre: 'Edificio El Cedro (Entrada Lateral)', tipo: 'ENTRADA_EDIFICIO', edificio: 'El Cedro' },
];

describe('Consulta', () => {
  let fixture: ComponentFixture<Consulta>;
  let elemento: HTMLElement;

  const boton = () => elemento.querySelector<HTMLButtonElement>('button[type=submit]')!;
  const select = (id: string) => elemento.querySelector<HTMLSelectElement>(`#${id}`)!;
  const elegir = async (id: string, valor: string) => {
    select(id).value = valor;
    select(id).dispatchEvent(new Event('change'));
    await fixture.whenStable();
  };

  beforeEach(async () => {
    fixture = TestBed.createComponent(Consulta);
    fixture.componentRef.setInput('puntos', PUNTOS);
    await fixture.whenStable();
    elemento = fixture.nativeElement;
  });

  it('deshabilita "Calcular ruta" sin origen ni destino', () => {
    expect(boton().textContent?.trim()).toBe('Calcular ruta');
    expect(boton().disabled).toBe(true);
  });

  it('sigue deshabilitado con solo el origen', async () => {
    await elegir('origen', '1');
    expect(boton().disabled).toBe(true);
  });

  it('se habilita con origen y destino y emite la solicitud', async () => {
    const emitidas: SolicitudRuta[] = [];
    fixture.componentInstance.calcular.subscribe((s) => emitidas.push(s));

    await elegir('origen', '1');
    await elegir('destino', '2');
    expect(boton().disabled).toBe(false);

    boton().click();
    expect(emitidas).toEqual([{ origen: 1, destino: 2, modo: 'directa' }]);
  });

  it('agrupa los puntos: entradas del campus primero y luego por edificio', () => {
    const grupos = [...select('origen').querySelectorAll('optgroup')].map((g) => g.label);
    expect(grupos).toEqual(['Entradas del campus', 'El Cedro']);
  });

  it('cada selector tiene una etiqueta asociada', () => {
    expect(elemento.querySelector('label[for=origen]')?.textContent?.trim()).toBe('Origen');
    expect(elemento.querySelector('label[for=destino]')?.textContent?.trim()).toBe('Destino');
  });

  it('"Intercambiar origen y destino" invierte la selección', async () => {
    await elegir('origen', '1');
    await elegir('destino', '2');
    const intercambiar = [...elemento.querySelectorAll('button')].find((b) => b.textContent?.includes('Intercambiar'))!;
    intercambiar.click();
    await fixture.whenStable();
    expect(select('origen').value).toBe('2');
    expect(select('destino').value).toBe('1');
  });
});
