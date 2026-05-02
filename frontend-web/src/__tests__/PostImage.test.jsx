/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { PostImage, Avatar } from '../components/ui/PostImage';

// Defines post image so related behavior stays grouped in one place.
describe('PostImage', () => {
  // Verifies renders placeholder when src is null so regressions are caught during automated tests.
  it('renders placeholder when src is null', () => {
    const { container } = render(<PostImage src={null} alt="test" />);
    // Should render a div fallback (not an img)
    expect(container.querySelector('img')).toBeNull();
  });

  // Verifies renders placeholder when src is empty string so regressions are caught during automated tests.
  it('renders placeholder when src is empty string', () => {
    const { container } = render(<PostImage src="" alt="test" />);
    expect(container.querySelector('img')).toBeNull();
  });

  // Verifies renders placeholder when src is not a valid url so regressions are caught during automated tests.
  it('renders placeholder when src is not a valid URL', () => {
    const { container } = render(<PostImage src="not-a-url" alt="test" />);
    expect(container.querySelector('img')).toBeNull();
  });

  // Verifies renders img when src starts with http:// so regressions are caught during automated tests.
  it('renders img when src starts with http://', () => {
    const { container } = render(<PostImage src="http://example.com/img.jpg" alt="test" />);
    const img = container.querySelector('img');
    expect(img).not.toBeNull();
    expect(img.src).toBe('http://example.com/img.jpg');
  });

  // Verifies renders img when src starts with https:// so regressions are caught during automated tests.
  it('renders img when src starts with https://', () => {
    const { container } = render(<PostImage src="https://example.com/img.jpg" alt="test" />);
    const img = container.querySelector('img');
    expect(img).not.toBeNull();
  });

  // Verifies renders img when src starts with / so regressions are caught during automated tests.
  it('renders img when src starts with /', () => {
    const { container } = render(<PostImage src="/images/test.jpg" alt="test" />);
    const img = container.querySelector('img');
    expect(img).not.toBeNull();
  });

  // Defines shows fallback after image error so related behavior stays grouped in one place.
  it('shows fallback after image error', () => {
    const { container } = render(<PostImage src="https://example.com/broken.jpg" alt="test" />);
    const img = container.querySelector('img');
    expect(img).not.toBeNull();
    fireEvent.error(img);
    // After error, img should be gone, replaced by fallback div
    expect(container.querySelector('img')).toBeNull();
  });

  // Defines applies class name so related behavior stays grouped in one place.
  it('applies className', () => {
    const { container } = render(<PostImage src={null} alt="test" className="custom-class" />);
    expect(container.firstChild.className).toContain('custom-class');
  });
});

// Defines avatar so related behavior stays grouped in one place.
describe('Avatar', () => {
  // Verifies renders initial when src is null so regressions are caught during automated tests.
  it('renders initial when src is null', () => {
    render(<Avatar src={null} name="John" />);
    expect(screen.getByText('J')).toBeInTheDocument();
  });

  // Verifies renders initial u when name is empty so regressions are caught during automated tests.
  it('renders initial U when name is empty', () => {
    render(<Avatar src={null} name="" />);
    expect(screen.getByText('U')).toBeInTheDocument();
  });

  // Verifies renders img when src is valid so regressions are caught during automated tests.
  it('renders img when src is valid', () => {
    const { container } = render(<Avatar src="https://example.com/avatar.jpg" name="Jane" />);
    const img = container.querySelector('img');
    expect(img).not.toBeNull();
    expect(img.alt).toBe('Jane');
  });

  // Defines falls back to initial on image error so related behavior stays grouped in one place.
  it('falls back to initial on image error', () => {
    const { container } = render(<Avatar src="https://example.com/broken.jpg" name="Dave" />);
    const img = container.querySelector('img');
    fireEvent.error(img);
    expect(screen.getByText('D')).toBeInTheDocument();
  });

  // Defines applies size sm so related behavior stays grouped in one place.
  it('applies size sm', () => {
    const { container } = render(<Avatar src={null} name="A" size="sm" />);
    expect(container.firstChild.className).toContain('h-8');
  });

  // Defines applies size lg so related behavior stays grouped in one place.
  it('applies size lg', () => {
    const { container } = render(<Avatar src={null} name="A" size="lg" />);
    expect(container.firstChild.className).toContain('h-16');
  });

  // Defines applies size xl so related behavior stays grouped in one place.
  it('applies size xl', () => {
    const { container } = render(<Avatar src={null} name="A" size="xl" />);
    expect(container.firstChild.className).toContain('h-24');
  });

  // Defines defaults to md size for unknown size so related behavior stays grouped in one place.
  it('defaults to md size for unknown size', () => {
    const { container } = render(<Avatar src={null} name="A" size="unknown" />);
    expect(container.firstChild.className).toContain('h-10');
  });
});
