/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, fireEvent } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';

// Mock the ReactQuill component since it requires DOM APIs not available in jsdom
vi.mock('react-quill-new', () => ({
  default: ({ value, onChange, theme, className }) => (
    <div data-testid="mock-quill" className={className}>
      <textarea
        data-testid="quill-textarea"
        value={value || ''}
        onChange={(e) => onChange?.(e.target.value)}
      />
    </div>
  ),
}));

vi.mock('react-quill-new/dist/quill.snow.css', () => ({}));

import { RichTextEditor } from '../components/forms/RichTextEditor';

// Defines rich text editor so related behavior stays grouped in one place.
describe('RichTextEditor', () => {
  // Verifies renders the editor so regressions are caught during automated tests.
  it('renders the editor', () => {
    render(<RichTextEditor value="" onChange={() => {}} />);
    expect(screen.getByTestId('mock-quill')).toBeInTheDocument();
  });

  // Defines passes value to react quill so related behavior stays grouped in one place.
  it('passes value to ReactQuill', () => {
    render(<RichTextEditor value="<p>Hello</p>" onChange={() => {}} />);
    expect(screen.getByTestId('quill-textarea').value).toBe('<p>Hello</p>');
  });

  // Defines calls on change when content changes so related behavior stays grouped in one place.
  it('calls onChange when content changes', () => {
    const handleChange = vi.fn();
    render(<RichTextEditor value="" onChange={handleChange} />);
    
    const textarea = screen.getByTestId('quill-textarea');
    fireEvent.change(textarea, { target: { value: '<p>New content</p>' } });
    
    expect(handleChange).toHaveBeenCalledWith('<p>New content</p>');
  });

  // Verifies renders with wrapper styling so regressions are caught during automated tests.
  it('renders with wrapper styling', () => {
    const { container } = render(<RichTextEditor value="" onChange={() => {}} />);
    const wrapper = container.firstChild;
    expect(wrapper.classList.contains('rounded-xl')).toBe(true);
  });
});
